package restaurant.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class WebConfig implements WebMvcConfigurer {
 private final RoleInterceptor interceptor;
 public WebConfig(RoleInterceptor interceptor){this.interceptor=interceptor;}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor).addPathPatterns("/**");}
}

