package com.hostelos.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React app (copied into classpath:/static at build time) from the same server as the API,
 * so a deployment is ONE web service and ONE URL. Client-side routes such as /meals or /admin/finance are
 * not files, so unknown non-API paths fall back to index.html and React Router takes over.
 *
 * In local development nothing is in /static (Vite serves the UI on :5173), so this is inert.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite fingerprints everything under /assets, so it can be cached "forever".
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())            // index.html must always be revalidated
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) return requested;
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("v3/")
                                || resourcePath.startsWith("swagger-ui") || resourcePath.contains(".")) {
                            return null;                                   // real 404 for API paths and missing files
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
