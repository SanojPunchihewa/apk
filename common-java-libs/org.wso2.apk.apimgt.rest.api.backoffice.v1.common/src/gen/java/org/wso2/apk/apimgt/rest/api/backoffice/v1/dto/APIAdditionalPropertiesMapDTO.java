package org.wso2.apk.apimgt.rest.api.backoffice.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;



public class APIAdditionalPropertiesMapDTO   {
  
    private String name = null;
    private String value = null;
    private Boolean display = false;

  /**
   **/
  public APIAdditionalPropertiesMapDTO name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   **/
  public APIAdditionalPropertiesMapDTO value(String value) {
    this.value = value;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("value")
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }

  /**
   **/
  public APIAdditionalPropertiesMapDTO display(Boolean display) {
    this.display = display;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("display")
  public Boolean isDisplay() {
    return display;
  }
  public void setDisplay(Boolean display) {
    this.display = display;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    APIAdditionalPropertiesMapDTO apIAdditionalPropertiesMap = (APIAdditionalPropertiesMapDTO) o;
    return Objects.equals(name, apIAdditionalPropertiesMap.name) &&
        Objects.equals(value, apIAdditionalPropertiesMap.value) &&
        Objects.equals(display, apIAdditionalPropertiesMap.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value, display);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIAdditionalPropertiesMapDTO {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
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

