/*
 *
 *  Copyright 2015 the original author or authors.
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

package springfox.documentation.schema;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.plugins.SchemaPluginsManager;
import springfox.documentation.schema.property.ModelPropertiesProvider;
import springfox.documentation.spi.schema.contexts.ModelContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Lists.*;
import static springfox.documentation.schema.Collections.*;
import static springfox.documentation.schema.Maps.*;
import static springfox.documentation.schema.ResolvedTypes.*;
import static springfox.documentation.schema.Types.*;


@Component
@Qualifier("default")
public class DefaultModelProvider implements ModelProvider {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultModelProvider.class);
  private final TypeResolver resolver;
  private final ModelDependencyProvider dependencyProvider;
  private final SchemaPluginsManager schemaPluginsManager;
  private final TypeNameExtractor typeNameExtractor;

  @Autowired
  public DefaultModelProvider(TypeResolver resolver,
      @Qualifier("cachedModelProperties") ModelPropertiesProvider propertiesProvider,
      @Qualifier("cachedModelDependencies") ModelDependencyProvider dependencyProvider,
      SchemaPluginsManager schemaPluginsManager,
      TypeNameExtractor typeNameExtractor) {
    this.resolver = resolver;
    this.dependencyProvider = dependencyProvider;
    this.schemaPluginsManager = schemaPluginsManager;
    this.typeNameExtractor = typeNameExtractor;
  }

  @Override
  public List<ModelContext> modelsFor(ModelContext modelContext) {
    List<ModelContext> modelContexts = newArrayList();
    if (shouldIgnore(modelContext)) {
      return modelContexts;    
    }
    for (ModelContext parentContext : FluentIterable.from(dependencyProvider.dependentModels(modelContext)).filter(shouldIgnore()).toList()) {
      Optional<Model> model = Optional.of(modelBuilder(parentContext)).or(mapModel(parentContext, parentContext.resolvedType(resolver)));
      if (model.isPresent()) {
          modelContexts.add(parentContext);
        LOG.debug("Generated parameter model id: {}, name: {}, schema: {} models",
          model.get().getId(),
          model.get().getName());
      }    
    }
    modelBuilder(modelContext);
    modelContexts.add(modelContext);
    return modelContexts;
  }

  private Model modelBuilder(ModelContext modelContext) {
    ResolvedType propertiesHost = modelContext.alternateFor(modelContext.resolvedType(resolver));
    String typeName = typeNameExtractor.typeName(ModelContext.fromParent(modelContext, propertiesHost));
    modelContext.getBuilder()
        .id(typeName)
        .type(propertiesHost)
        .name(typeName)
        .index(0)
        .qualifiedType(simpleQualifiedTypeName(propertiesHost))
        .description("")
        .baseModel("")
        .discriminator("")
        .subTypes(new ArrayList<String>());
    return schemaPluginsManager.model(modelContext);
  }
  
  private Optional<Model> mapModel(ModelContext parentContext, ResolvedType resolvedType) {
    if (isMapType(resolvedType) && !parentContext.hasSeenBefore(resolvedType)) {
      String typeName = typeNameExtractor.typeName(parentContext);
      return Optional.of(parentContext.getBuilder()
          .id(typeName)
          .type(resolvedType)
          .name(typeName)
          .index(0)
          .qualifiedType(simpleQualifiedTypeName(resolvedType))
          .properties(new HashMap<String, ModelProperty>())
          .description("")
          .baseModel("")
          .discriminator("")
          .subTypes(new ArrayList<String>())
          .build());
    }
    return Optional.absent();
  }
  
  private Predicate<ModelContext> shouldIgnore() {
      return new Predicate<ModelContext>() {
        @Override
        public boolean apply(ModelContext input) {
          return shouldIgnore(input);       
        }      
      };
    }
  
  private boolean shouldIgnore(ModelContext context) {
    ResolvedType propertiesHost = context.alternateFor(context.resolvedType(resolver));
    if (isContainerType(propertiesHost)
        || isMapType(propertiesHost)
        || propertiesHost.getErasedType().isEnum()
        || isBaseType(propertiesHost)
        || context.hasSeenBefore(propertiesHost)) {
      LOG.debug("Skipping model of type {} as its either a container type, map, enum or base type, or its already "
          + "been handled", resolvedTypeSignature(propertiesHost).or("<null>"));  
      return true;
    }
    return false; 
  }
     
}
