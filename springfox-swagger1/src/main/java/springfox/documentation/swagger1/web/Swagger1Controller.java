/*
 *
 *  Copyright 2017 the original author or authors.
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

package springfox.documentation.swagger1.web;

import static springfox.documentation.swagger1.web.ApiListingMerger.mergedApiListing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.service.Documentation;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.PropertySourcedMapping;
import springfox.documentation.spring.web.json.Json;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger1.dto.ApiListing;
import springfox.documentation.swagger1.dto.ResourceListing;
import springfox.documentation.swagger1.mappers.ServiceModelToSwaggerMapper;

@Controller
@ApiIgnore
public class Swagger1Controller {

  private final DocumentationCache documentationCache;
  private final ServiceModelToSwaggerMapper mapper;
  private final JsonSerializer jsonSerializer;

  @Autowired
  public Swagger1Controller(
      DocumentationCache documentationCache,
      ServiceModelToSwaggerMapper mapper,
      JsonSerializer jsonSerializer) {

    this.documentationCache = documentationCache;
    this.mapper = mapper;
    this.jsonSerializer = jsonSerializer;
  }

  @RequestMapping(value = "/api-docs", method = RequestMethod.GET)
  @PropertySourcedMapping(
      value = "${springfox.documentation.swagger.v1.path}",
      propertyKey = "springfox.documentation.swagger.v1.path")
  @ResponseBody
  public ResponseEntity<Json> getResourceListing(
      @RequestParam(value = "group", required = false) String swaggerGroup) {

    return getSwaggerResourceListing(swaggerGroup);
  }

  @RequestMapping(value = "/api-docs/{swaggerGroup}/{apiDeclaration}", method = RequestMethod.GET)
  @PropertySourcedMapping(
      value = "${springfox.documentation.swagger.v1.path}/{swaggerGroup}/{apiDeclaration}",
      propertyKey = "springfox.documentation.swagger.v1.path")
  @ResponseBody
  public ResponseEntity<Json> getApiListing(
      @PathVariable String swaggerGroup,
      @PathVariable String apiDeclaration) {

    return getSwaggerApiListing(swaggerGroup, apiDeclaration);
  }

  private ResponseEntity<Json> getSwaggerApiListing(String swaggerGroup, String apiDeclaration) {
    String groupName = Optional.ofNullable(swaggerGroup).orElse("default");
    Documentation documentation = documentationCache.documentationByGroup(groupName);
    if (documentation == null) {
      return new ResponseEntity<Json>(HttpStatus.NOT_FOUND);
    }
    Map<String, List<springfox.documentation.service.ApiListing>> apiListingMap = documentation.getApiListings();
    
    Map<String, List<ApiListing>> dtoApiListings = apiListingMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, 
          e -> {
            List<ApiListing> mapped = new ArrayList<>();
            for(springfox.documentation.service.ApiListing each : e.getValue()) {
              mapped.add(mapper.toSwaggerApiListing(each));
            }
            return mapped;
          }));
    
    List<ApiListing> apiListings = dtoApiListings.get(apiDeclaration);
    return mergedApiListing(apiListings)
        .map(toJson())
        .map(toResponseEntity(Json.class))
        .orElse(new ResponseEntity<Json>(HttpStatus.NOT_FOUND));
  }

  private Function<ApiListing, Json> toJson() {
    return new Function<ApiListing, Json>() {
      @Override
      public Json apply(ApiListing input) {
        return jsonSerializer.toJson(input);
      }
    };
  }

  private ResponseEntity<Json> getSwaggerResourceListing(String swaggerGroup) {
    String groupName = Optional.ofNullable(swaggerGroup).orElse(Docket.DEFAULT_GROUP_NAME);
    Documentation documentation = documentationCache.documentationByGroup(groupName);
    if (documentation == null) {
      return new ResponseEntity<Json>(HttpStatus.NOT_FOUND);
    }
    springfox.documentation.service.ResourceListing listing = documentation.getResourceListing();
    ResourceListing resourceListing = mapper.toSwaggerResourceListing(listing);

    return Optional.ofNullable(jsonSerializer.toJson(resourceListing))
        .map(toResponseEntity(Json.class))
        .orElse(new ResponseEntity<Json>(HttpStatus.NOT_FOUND));
  }

  private <T> Function<T, ResponseEntity<T>> toResponseEntity(Class<T> clazz) {
    return new Function<T, ResponseEntity<T>>() {
      @Override
      public ResponseEntity<T> apply(T input) {
        return new ResponseEntity<T>(input, HttpStatus.OK);
      }
    };
  }
}
