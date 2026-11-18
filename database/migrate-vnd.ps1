# One-time conversion of an existing, unconverted database. New installs use VND seeds.
[CmdletBinding()]
param(
    [string]$ProjectName = '',
    [string]$EnvFile = ''
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$compose = @('compose', '--project-directory', $root)
if ($ProjectName) { $compose += @('--project-name', $ProjectName) }
if ($EnvFile) { $compose += @('--env-file', (Resolve-Path -LiteralPath $EnvFile).Path) }
$utf8 = New-Object System.Text.UTF8Encoding($false)
$culture = [System.Globalization.CultureInfo]::InvariantCulture
$OutputEncoding = $utf8
[Console]::OutputEncoding = $utf8

function Invoke-DockerText([string[]]$Arguments, [string]$InputText = '') {
    $result = $InputText | & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'Docker/MySQL command failed; conversion was not confirmed.' }
    return ($result -join "`n")
}

function Invoke-Sql([string]$Sql) {
    $client = 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=TCP -h127.0.0.1 --user="$MYSQL_USER" "$MYSQL_DATABASE" --default-character-set=utf8mb4 -N -B'
    return Invoke-DockerText ($compose + @('exec', '-T', 'db', 'sh', '-c', $client)) $Sql
}

$services = Invoke-DockerText ($compose + @('ps', '--status', 'running', '--services'))
if (($services -split "`n") -contains 'app') {
    throw 'Stop the app service before converting money. Keep the db service running.'
}
$identity = (Invoke-Sql 'SELECT @@server_uuid, DATABASE();').Trim()
if ($identity -notmatch '^([a-fA-F0-9-]{36})\t([a-zA-Z0-9_]+)$') {
    throw 'Cannot identify the target MySQL database safely.'
}
$databaseId = $Matches[1] + '-' + $Matches[2]
$folder = Join-Path $PSScriptRoot ('backups/vnd-10000-' + $databaseId)
$receipt = Join-Path $folder 'completed.json'
if (Test-Path -LiteralPath $receipt) {
    Write-Host 'VND conversion already completed for this database. No amounts changed.'
    exit 0
}
New-Item -ItemType Directory -Path $folder -Force | Out-Null
$manifestPath = Join-Path $folder 'before.json'
$backupPath = Join-Path $folder 'before.sql'
$sqlPath = Join-Path $folder 'convert.sql'
$targets = @(
    @{ Table = 'Menu_item'; Key = 'item_id'; Money = 'unit_price' },
    @{ Table = 'Orders'; Key = 'order_id'; Money = 'total_amount' },
    @{ Table = 'Order_Detail'; Key = 'order_detail_id'; Money = 'unit_price_at_order' },
    @{ Table = 'Payment'; Key = 'payment_id'; Money = 'amount' },
    @{ Table = 'Invoice'; Key = 'invoice_id'; Money = 'total_amount' }
)

if (!(Test-Path -LiteralPath $manifestPath)) {
    # Do not print credentials. The dump remains local and is excluded from Git/Docker.
    $dumpCommand = 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --protocol=TCP -h127.0.0.1 --user="$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF --skip-comments --complete-insert --default-character-set=utf8mb4 "$MYSQL_DATABASE"'
    $dump = Invoke-DockerText ($compose + @('exec', '-T', 'db', 'sh', '-c', $dumpCommand))
    if (!$dump.Contains('CREATE TABLE') -or !$dump.Contains('INSERT INTO')) {
        throw 'The database backup is empty or incomplete.'
    }
    [System.IO.File]::WriteAllText($backupPath, $dump + "`n", $utf8)
    $tables = @()
    foreach ($target in $targets) {
        $rows = @()
        $values = Invoke-Sql "SELECT $($target.Key), $($target.Money) FROM $($target.Table) ORDER BY $($target.Key);"
        foreach ($line in ($values -split "`n" | Where-Object { $_ })) {
            $parts = $line.Split("`t")
            if ($parts.Length -ne 2 -or $parts[0] -notmatch '^\d+$') { throw 'Invalid monetary record.' }
            $old = [decimal]::Parse($parts[1], $culture)
            $converted = $old * 10000
            if ($old -lt 0 -or $converted -gt [decimal]9999999999.99) {
                throw "Conversion exceeds DECIMAL(12,2) in $($target.Table); no amounts changed."
            }
            $rows += @{ Id = $parts[0]; Old = $old.ToString('0.00', $culture) }
        }
        $tables += @{ Table = $target.Table; Key = $target.Key; Money = $target.Money; Rows = $rows }
    }
    $manifest = @{ Database = $identity; Factor = 10000; Tables = $tables; BackupSha256 = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash }
    [System.IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 8), $utf8)
}

$saved = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
if ($saved.Database -ne $identity -or $saved.Factor -ne 10000 -or
    (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash -ne $saved.BackupSha256) {
    throw 'Database identity or backup checksum does not match the saved conversion.'
}

# The saved baseline is created once. A retry accepts either the entire old state
# or the entire converted state, never a mixture, and never includes new records.
$sql = [System.Collections.Generic.List[string]]::new()
$sql.Add('SET SESSION sql_mode = ''STRICT_ALL_TABLES'';')
$sql.Add('CREATE TEMPORARY TABLE vnd_guard (ok INT NOT NULL CHECK (ok = 1));')
foreach ($table in $saved.Tables) {
    $sql.Add("CREATE TEMPORARY TABLE vnd_$($table.Table) (id INT PRIMARY KEY, amount DECIMAL(12,2) NOT NULL);")
    if ($table.Rows.Count) {
        $tuples = @($table.Rows | ForEach-Object { "($($_.Id),$($_.Old))" }) -join ','
        $sql.Add("INSERT INTO vnd_$($table.Table) VALUES $tuples;")
    }
}
$sql.Add('START TRANSACTION;')
$oldChecks = @()
$newChecks = @()
foreach ($table in $saved.Tables) {
    $t = $table.Table; $k = $table.Key; $m = $table.Money
    $sql.Add("SELECT $k FROM $t ORDER BY $k FOR UPDATE;")
    $count = @($table.Rows).Count
    $prefix = "(SELECT COUNT(*) FROM $t a JOIN vnd_$t b ON a.$k = b.id WHERE a.$m = b.amount"
    $oldChecks += "$prefix) = $count"
    $newChecks += "$prefix * 10000) = $count"
    # Row counts reject records inserted/deleted between backup and commit.
    $oldChecks += "(SELECT COUNT(*) FROM $t) = $count"
    $newChecks += "(SELECT COUNT(*) FROM $t) = $count"
}
$sql.Add('SET @vnd_old = ' + ($oldChecks -join ' AND ') + ';')
$sql.Add('SET @vnd_new = ' + ($newChecks -join ' AND ') + ';')
$sql.Add('INSERT INTO vnd_guard VALUES (IF(@vnd_old OR @vnd_new, 1, 0));')
foreach ($table in $saved.Tables) {
    $sql.Add("UPDATE $($table.Table) a JOIN vnd_$($table.Table) b ON a.$($table.Key) = b.id SET a.$($table.Money) = b.amount * 10000 WHERE @vnd_old;")
}
$sql.Add('INSERT INTO vnd_guard VALUES (IF(' + ($newChecks -join ' AND ') + ', 1, 0));')
$sql.Add('COMMIT;')
[System.IO.File]::WriteAllText($sqlPath, ($sql -join "`n") + "`n", $utf8)
Invoke-Sql ([System.IO.File]::ReadAllText($sqlPath, $utf8)) | Out-Null
$done = @{ Database = $identity; Factor = 10000; CompletedUtc = [DateTime]::UtcNow.ToString('o'); BackupSha256 = $saved.BackupSha256 }
[System.IO.File]::WriteAllText($receipt, ($done | ConvertTo-Json), $utf8)
Write-Host 'Converted all five money columns to VND (x10,000). Backup and retry journal:'
Write-Host $folder
