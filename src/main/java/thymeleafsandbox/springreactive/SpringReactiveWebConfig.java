/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2014, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package thymeleafsandbox.springreactive;


import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.freemarker.SpringTemplateLoader;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.context.reactive.SpringReactiveEngineContextFactory;
import org.thymeleaf.spring5.linkbuilder.reactive.SpringReactiveLinkBuilder;
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

@Configuration
@EnableConfigurationProperties(ThymeleafProperties.class)
public class SpringReactiveWebConfig {

    // TODO * Once there is a Spring Boot starter for thymeleaf-spring5, there would be no need to have
    // TODO   that @EnableConfigurationProperties annotation or use it for declaring the beans down in the
    // TODO   "thymeleaf" section below.


    private ApplicationContext applicationContext;
    private ThymeleafProperties thymeleafProperties;



    public SpringReactiveWebConfig(
            final ApplicationContext applicationContext,
            final ThymeleafProperties thymeleafProperties) {
        super();
        this.applicationContext = applicationContext;
        this.thymeleafProperties = thymeleafProperties;
    }




    /*
     * --------------------------------------
     * FREEMARKER CONFIGURATION
     * --------------------------------------
     */


    @Bean
    public FreeMarkerConfigurer freeMarkerConfig() {
        // Note this is the reactive version of FreeMarker's configuration, so there is no auto-configuration yet.
        final FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
        freeMarkerConfigurer.setPreTemplateLoaders(new SpringTemplateLoader(this.applicationContext, "/templates/"));
        return freeMarkerConfigurer;
    }

    /*
     * ViewResolver for FreeMarker templates executing in NORMAL mode (only mode available for FreeMarker)
     * No limit to output buffer size, all data fully resolved in context.
     */
    @Bean
    public FreeMarkerViewResolver freeMarkerViewResolver() {
        final FreeMarkerViewResolver freeMarkerViewResolver = new FreeMarkerViewResolver("", ".ftl");
        freeMarkerViewResolver.setOrder(4);
        // TODO * Apparently no way to specify which views can be handled by this ViewResolver (viewNames property)
        return freeMarkerViewResolver;
    }





    /*
     * --------------------------------------
     * THYMELEAF CONFIGURATION
     * --------------------------------------
     */

    // TODO * If there was a Spring Boot starter for thymeleaf-spring5 most probably some or all of these
    // TODO   resolver and engine beans would not beed to be specifically declared here.

    @Bean
    public SpringResourceTemplateResolver thymeleafTemplateResolver() {

        final SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(this.applicationContext);
        resolver.setPrefix(this.thymeleafProperties.getPrefix());
        resolver.setSuffix(this.thymeleafProperties.getSuffix());
        resolver.setTemplateMode(this.thymeleafProperties.getMode());
        if (this.thymeleafProperties.getEncoding() != null) {
            resolver.setCharacterEncoding(this.thymeleafProperties.getEncoding().name());
        }
        resolver.setCacheable(this.thymeleafProperties.isCache());
        final Integer order = this.thymeleafProperties.getTemplateResolverOrder();
        if (order != null) {
            resolver.setOrder(order);
        }
        resolver.setCheckExistence(this.thymeleafProperties.isCheckTemplate());

        return resolver;

    }



    @Bean
    public SpringTemplateEngine thymeleafTemplateEngine(){
        // We override here the SpringTemplateEngine instance that would otherwise be instantiated by
        // Spring Boot because we want to apply the SpringReactive-specific context factory, link builder...
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(thymeleafTemplateResolver());
        templateEngine.setEngineContextFactory(new SpringReactiveEngineContextFactory());
        templateEngine.setLinkBuilder(new SpringReactiveLinkBuilder());
        return templateEngine;
    }

    /*
     * ViewResolver for Thymeleaf templates executing in NORMAL mode.
     * No limit to output buffer size, non-data-driven (all data fully resolved in context).
     */
    @Bean
    public ThymeleafReactiveViewResolver thymeleafNormalViewResolver(){
        ThymeleafReactiveViewResolver viewResolver = new ThymeleafReactiveViewResolver();
        viewResolver.setTemplateEngine(thymeleafTemplateEngine());
        viewResolver.setOrder(3);
        viewResolver.setViewNames(new String[] {"thymeleaf/*"});
        return viewResolver;
    }

    /*
     * ViewResolver for Thymeleaf templates executing in BUFFERED mode.
     * Non-data-driven (all data fully resolved in context), but with an established limit to output buffers size.
     */
    @Bean
    public ThymeleafReactiveViewResolver thymeleafBufferedViewResolver(){
        ThymeleafReactiveViewResolver viewResolver = new ThymeleafReactiveViewResolver();
        viewResolver.setTemplateEngine(thymeleafTemplateEngine());
        viewResolver.setOrder(2);
        viewResolver.setViewNames(new String[] {"thymeleaf/*buffered*"});
        viewResolver.setResponseMaxBufferSizeBytes(16384); // OUTPUT BUFFER size limit
        return viewResolver;
    }

    /*
     * ViewResolver for Thymeleaf templates executing in NORMAL mode
     * Data-driven: the "dataSource" variable can be a Publisher<X>, in which case it will drive the execution of
     * the engine and Thymeleaf will be executed as a part of the data flow.
     */
    @Bean
    public ThymeleafReactiveViewResolver thymeleafDataDrivenViewResolver(){
        ThymeleafReactiveViewResolver viewResolver = new ThymeleafReactiveViewResolver();
        viewResolver.setTemplateEngine(thymeleafTemplateEngine());
        viewResolver.setOrder(1);
        viewResolver.setViewNames(new String[] {"thymeleaf/*datadriven*"});
        viewResolver.setResponseMaxBufferSizeBytes(16384); // OUTPUT BUFFER size limit
        viewResolver.setDataDrivenVariableName("dataSource"); // Name of the Publisher<X> that will DRIVE execution
        viewResolver.setDataDrivenChunkSizeElements(1000); // Size (in elements) of the chunks of published data to be processed
        return viewResolver;
    }

}
