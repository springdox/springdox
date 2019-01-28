/*
 *
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package springfox.documentation.uploader.annotations;

import org.springframework.context.annotation.Import;
import springfox.documentation.swagger2.configuration.Swagger2DocumentationWebMvcConfiguration;
import springfox.documentation.uploader.spring.FileUploaderBeanConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that auto upload should be enabled. This should be applied to a Spring Java config file and should have an
 * accompanying '@Configuration' annotation. Loads all required beans defined in @see FileUploaderBeanConfiguration.
 *
 * @author Esteban Cristóbal Rodríguez
 **/
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = {java.lang.annotation.ElementType.TYPE})
@Documented
@Import({ FileUploaderBeanConfiguration.class, Swagger2DocumentationWebMvcConfiguration.class })
public @interface EnableSwaggerUpload {

}
