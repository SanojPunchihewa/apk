package org.wso2.apk.apimgt.rest.api.backoffice.v1.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;



public class APIDefinitionDTO   {
  

    @XmlType(name="TypeEnum")
    @XmlEnum(String.class)
    public enum TypeEnum {
        SWAGGER("swagger"),
        GRAPHQL("graphql"),
        WSDL("wsdl"),
        ASYNC("async");
        private String value;

        TypeEnum (String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static TypeEnum fromValue(String v) {
            for (TypeEnum b : TypeEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
return null;
        }
    }
    private TypeEnum type = null;
    private String schemaDefinition = null;

  /**
   **/
  public APIDefinitionDTO type(TypeEnum type) {
    this.type = type;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type")
  @NotNull
  public TypeEnum getType() {
    return type;
  }
  public void setType(TypeEnum type) {
    this.type = type;
  }

  /**
   **/
  public APIDefinitionDTO schemaDefinition(String schemaDefinition) {
    this.schemaDefinition = schemaDefinition;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("schemaDefinition")
  public String getSchemaDefinition() {
    return schemaDefinition;
  }
  public void setSchemaDefinition(String schemaDefinition) {
    this.schemaDefinition = schemaDefinition;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    APIDefinitionDTO apIDefinition = (APIDefinitionDTO) o;
    return Objects.equals(type, apIDefinition.type) &&
        Objects.equals(schemaDefinition, apIDefinition.schemaDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, schemaDefinition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIDefinitionDTO {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    schemaDefinition: ").append(toIndentedString(schemaDefinition)).append("\n");
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

