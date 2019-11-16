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

import com.fasterxml.jackson.annotation.JsonProperty;

public class TypeWithConstructorProperties {
  private String foo;
  private Integer bar;

  public TypeWithConstructorProperties(@JsonProperty(value = "foo", required = true) String foo, @JsonProperty(value = "bar", required = true) Integer bar) {
    this.foo = foo;
    this.bar = bar;
  }

  public String getFoo() {
    return foo;
  }

  public Integer getBar() {
    return bar;
  }
}
