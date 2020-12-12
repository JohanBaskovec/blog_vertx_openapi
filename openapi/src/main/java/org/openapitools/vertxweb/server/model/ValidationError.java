package org.openapitools.vertxweb.server.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError   {
  
  private String message;
  private String parameterName;

  public ValidationError () {

  }

  public ValidationError (String message, String parameterName) {
    this.message = message;
    this.parameterName = parameterName;
  }

    
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }

    
  @JsonProperty("parameterName")
  public String getParameterName() {
    return parameterName;
  }
  public void setParameterName(String parameterName) {
    this.parameterName = parameterName;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidationError validationError = (ValidationError) o;
    return Objects.equals(message, validationError.message) &&
        Objects.equals(parameterName, validationError.parameterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, parameterName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValidationError {\n");
    
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    parameterName: ").append(toIndentedString(parameterName)).append("\n");
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
