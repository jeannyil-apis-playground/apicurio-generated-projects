
package org.github.jeannyil.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Validation Result   
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "validationResult"
})
public class ValidationResult {

    @JsonProperty("validationResult")
    private ValidationResult__1 validationResult;

    @JsonProperty("validationResult")
    public ValidationResult__1 getValidationResult() {
        return validationResult;
    }

    @JsonProperty("validationResult")
    public void setValidationResult(ValidationResult__1 validationResult) {
        this.validationResult = validationResult;
    }

}
