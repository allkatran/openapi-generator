package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;


import java.util.*;
import javax.annotation.Generated;

/**
 * HasOnlyReadOnlyDto
 */

@JsonTypeName("hasOnlyReadOnly")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class HasOnlyReadOnlyDto {

  private @Nullable String bar;

  private @Nullable String foo;

  public HasOnlyReadOnlyDto bar(@Nullable String bar) {
    this.bar = bar;
    return this;
  }

  /**
   * Get bar
   * @return bar
   */
  
  @ApiModelProperty(readOnly = true, value = "")
  @JsonProperty("bar")
  public @Nullable String getBar() {
    return bar;
  }

  public void setBar(@Nullable String bar) {
    this.bar = bar;
  }

  public HasOnlyReadOnlyDto foo(@Nullable String foo) {
    this.foo = foo;
    return this;
  }

  /**
   * Get foo
   * @return foo
   */
  
  @ApiModelProperty(readOnly = true, value = "")
  @JsonProperty("foo")
  public @Nullable String getFoo() {
    return foo;
  }

  public void setFoo(@Nullable String foo) {
    this.foo = foo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HasOnlyReadOnlyDto hasOnlyReadOnly = (HasOnlyReadOnlyDto) o;
    return Objects.equals(this.bar, hasOnlyReadOnly.bar) &&
        Objects.equals(this.foo, hasOnlyReadOnly.foo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bar, foo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HasOnlyReadOnlyDto {\n");
    sb.append("    bar: ").append(toIndentedString(bar)).append("\n");
    sb.append("    foo: ").append(toIndentedString(foo)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

