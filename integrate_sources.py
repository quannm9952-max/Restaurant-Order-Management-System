from pathlib import Path
import shutil, re

ROOT = Path(__file__).resolve().parent
assert ROOT.name == 'rom-system-integration'
def safe(p):
    p = (ROOT / p).resolve()
    assert p.is_relative_to(ROOT), p
    return p
def write(p, text):
    p = safe(p); p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8', newline='\n')
def copytree(src, dst, transform=lambda s:s):
    for p in safe(src).rglob('*'):
        if p.is_file():
            out = safe(dst) / p.relative_to(safe(src)); out.parent.mkdir(parents=True, exist_ok=True)
            if p.suffix in ('.java','.html','.css','.js','.properties'):
                out.write_text(transform(p.read_text(encoding='utf-8-sig')), encoding='utf-8', newline='\n')
            else: shutil.copy2(p, out)
def edit(p, fn):
    p=safe(p); p.write_text(fn(p.read_text(encoding='utf-8-sig')), encoding='utf-8', newline='\n')

# Preserve each member's module, with one application and one source/resource tree.
copytree('restaurantManagement/src/main/java/com/example/restaurantmanagement','backend/src/main/java/com/restaurant',lambda s:s.replace('com.example.restaurantmanagement','com.restaurant').replace('.reponsitories','.repository').replace('.exeptions','.exceptions'))
for old,new in [('reponsitories','repository'),('exeptions','exceptions')]:
    safe('backend/src/main/java/com/restaurant/'+old).rename(safe('backend/src/main/java/com/restaurant/'+new))
copytree('restaurantManagement/src/test/java/com/example/restaurantmanagement/services','backend/src/test/java/com/restaurant/services',lambda s:s.replace('com.example.restaurantmanagement','com.restaurant').replace('.reponsitories','.repository').replace('.exeptions','.exceptions'))
copytree('src/main/java/com/group09/restaurant','backend/src/main/java/com/restaurant/auth',lambda s:s.replace('com.group09.restaurant','com.restaurant.auth').replace('com.restaurant.auth.entity.Account','com.restaurant.entities.Account').replace('com.restaurant.auth.entity.Employee','com.restaurant.entities.Employee').replace('com.restaurant.auth.entity.Role','com.restaurant.entities.enums.Role').replace('com.restaurant.auth.repository','com.restaurant.repository'))
copytree('src/main/resources/templates/employees','frontend/templates/employees')
for f in ['login.html','access-denied.html']:
    write('frontend/templates/'+f,safe('src/main/resources/templates/'+f).read_text(encoding='utf-8-sig'))
copytree('restaurantManagement/src/main/resources/templates','frontend/templates')
copytree('restaurantManagement/src/main/resources/static','frontend/static/orders')
copytree('src/main/resources/static/images','frontend/static/images')
write('frontend/static/css/menu-payment.css',safe('src/main/resources/static/css/app.css').read_text(encoding='utf-8-sig'))
for f in ['menu.html','payments.html','invoice.html']:
    write('frontend/templates/'+f,safe('src/main/resources/templates/'+f).read_text(encoding='utf-8-sig').replace('/css/app.css','/css/menu-payment.css'))
for cls,svc in [('MenuController','MenuService'),('PaymentController','PaymentService')]:
    s=safe('src/main/java/restaurant/controller/'+cls+'.java').read_text(encoding='utf-8-sig')
    s=s.replace('package restaurant.controller','package com.restaurant.checkout.controller').replace('import restaurant.service.RestaurantService;',f'import com.restaurant.checkout.service.{svc};').replace('RestaurantService',svc)
    # Identity is supplied by shared Spring Security model advice, not a parallel session login.
    s=re.sub(r'\s*@SessionAttribute\("user"\) Map<String, Object> user,','',s)
    s=s.replace('        model.addAttribute("user", user);\n','')
    # TV5 owns Manager transaction history; keep the old URL as a redirect only.
    if cls=='PaymentController':
        start=s.index('    @GetMapping("/history")')
        s=s[:start]+'''    @GetMapping("/history")
    public String paymentHistory() { return "redirect:/manager/payment-history"; }
}
'''
    write('backend/src/main/java/com/restaurant/checkout/controller/'+cls+'.java',s)

# Use TV1 Account/Employee models, mapped to the approved FK direction.
for cls in ['Account','Employee']:
    s=safe('src/main/java/com/group09/restaurant/entity/'+cls+'.java').read_text(encoding='utf-8-sig').replace('package com.group09.restaurant.entity;', 'package com.restaurant.entities;\nimport com.restaurant.entities.enums.Role;')
    if cls=='Account':
        s=s.replace('\n}', '\n    public Integer getId() { return accountId; }\n    public void setId(Integer id) { accountId = id; }\n    public Boolean getIsActive() { return active; }\n    public void setIsActive(Boolean value) { active = Boolean.TRUE.equals(value); }\n}')
    else:
        s=s.replace('\n}', '\n    public Integer getId() { return employeeId; }\n    public void setId(Integer id) { employeeId = id; }\n    @jakarta.persistence.OneToOne(mappedBy = "employee")\n    private Account account;\n    public Account getAccount() { return account; }\n    public void setAccount(Account value) { account = value; }\n}')
    write('backend/src/main/java/com/restaurant/entities/'+cls+'.java',s)
# Keep one copy of auth repositories.
copytree('src/main/java/com/group09/restaurant/repository','backend/src/main/java/com/restaurant/repository',lambda s:s.replace('com.group09.restaurant.repository','com.restaurant.repository').replace('com.group09.restaurant.entity','com.restaurant.entities'))
edit('backend/src/main/java/com/restaurant/repository/EmployeeRepository.java',lambda s:s.replace('public interface EmployeeRepository extends JpaRepository<Employee, Integer> {','''public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    @org.springframework.data.jpa.repository.Query("select a.employee from Account a where a.username = :username")
    java.util.Optional<Employee> findByAccountUsername(@org.springframework.data.repository.query.Param("username") String username);
'''))

# Correct mappings without adding or renaming any approved database columns.
replacements={
 'RestaurantTable': [('@Table(name = "restaurant_table")','@Table(name = "Restaurant_tables")')],
 'MenuItem': [('@Column(name = "price"','@Column(name = "unit_price"'),('max = 100','max = 150'),('length = 100','length = 150')],
 'Order': [('@Table(name = "orders")','@Table(name = "Orders")'),('@Column(name = "note")','@Column(name = "notes")')],
 'OrderDetail': [('@Column(name = "cooking_status"','@Column(name = "preparation_status"'),('max = 100','max = 150'),('length = 100','length = 150')],
 'Payment': [('@Column(name = "payment_date"','@Column(name = "payment_time"'),('@Column(name = "method"','@Column(name = "payment_method"')],
}
for cls,rs in replacements.items():
    def fix(s):
        for a,b in rs:s=s.replace(a,b)
        if cls=='MenuItem':s=s.replace('    private Boolean isAvailable = true;','    private Boolean isAvailable = true;\n\n    @Column(name = "is_active", nullable = false)\n    private Boolean isActive = true;')
        return s
    edit('backend/src/main/java/com/restaurant/entities/'+cls+'.java',fix)
# Explicit canonical table names for every entity.
for cls,table in [('MenuCategory','Menu_category'),('MenuItem','Menu_item'),('OrderDetail','Order_Detail'),('Payment','Payment'),('Invoice','Invoice')]:
    edit('backend/src/main/java/com/restaurant/entities/'+cls+'.java',lambda s,t=table:s.replace('@Entity\n','@Entity\n@Table(name = "'+t+'")\n'))
edit('backend/src/main/java/com/restaurant/repository/MenuItemRepository.java',lambda s:s.replace('List<MenuItem> findByIsAvailableTrue();','@org.springframework.data.jpa.repository.Query("select m from MenuItem m where m.isAvailable = true and m.isActive = true")\n    List<MenuItem> findByIsAvailableTrue();'))

# TV2 operations are now protected by TV1 identity instead of the development stub.
for cls in ['order/impl/OrderServiceImpl','table/impl/TableServiceImpl']:
    p='backend/src/main/java/com/restaurant/services/'+cls+'.java'
    edit(p,lambda s:re.sub(r'^.*TODO\(auth\).*\n','',s,flags=re.M).replace('@Service\n','@Service\n@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'WAITER\',\'MANAGER\')")\n'))
edit('backend/src/main/java/com/restaurant/services/order/impl/OrderServiceImpl.java',lambda s:re.sub(r'(    public (?:Order|OrderDetail) (?:createOrder|addItemToOrder|reduceItemQuantity|cancelOrderDetail|cancelOrder)\()',r'    @org.springframework.security.access.prepost.PreAuthorize("hasRole(\'WAITER\')")\n\1',s).replace("\\'", "'"))

# Avoid Spring bean-name collisions between shared JPA repositories and read-only reports.
for tree in ['backend/src/main/java/com/restaurant/ordermanagement/reporting','backend/src/test/java/com/restaurant/ordermanagement/reporting']:
    for p in safe(tree).rglob('*.java'):
        s=p.read_text(encoding='utf-8-sig').replace('OrderRepository','OrderReportingRepository').replace('PaymentRepository','PaymentReportingRepository')
        p.write_text(s,encoding='utf-8',newline='\n')
for cls in ['Order','Payment']:
    safe(f'backend/src/main/java/com/restaurant/ordermanagement/reporting/repository/{cls}Repository.java').rename(safe(f'backend/src/main/java/com/restaurant/ordermanagement/reporting/repository/{cls}ReportingRepository.java'))

# Legacy dev-only entry points/configuration cannot coexist with the integrated application.
for rel in ['backend/src/main/java/com/restaurant/RestaurantManagementApplication.java','backend/src/main/java/com/restaurant/config/SecurityConfig.java','backend/src/main/java/com/restaurant/auth/RestaurantApplication.java','backend/src/main/java/com/restaurant/ordermanagement/RestaurantOrderManagementApplication.java','backend/src/main/java/com/restaurant/ordermanagement/reporting/config/ReportingSecurityConfiguration.java','backend/src/main/java/com/restaurant/ordermanagement/reporting/config/ReportingDemoSecurityConfiguration.java','backend/src/test/java/com/restaurant/ordermanagement/reporting/ReportingDemoSecurityTests.java','backend/src/main/resources/application-tv5-demo.properties']:
    p=safe(rel)
    if p.exists():p.unlink()
for rel in ['backend/src/main/java/com/restaurant/auth/entity','backend/src/main/java/com/restaurant/auth/repository','backend/src/main/java/com/restaurant/auth/config','src','restaurantManagement']:
    p=safe(rel)
    if p.exists():shutil.rmtree(p)
for rel in ['pom.xml','mvnw','mvnw.cmd']:
    p=safe(rel)
    if p.exists():p.unlink()
p=safe('.mvn')
if p.exists():shutil.rmtree(p)
print('Consolidated member source trees into backend/frontend with canonical entity mappings.')
