
package org.github.jeannyil.beans;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "status",
    "errorMessage"
})
public class ValidationResult__1 {

    @JsonProperty("status")
    private ValidationResult__1 .Status status;
    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("status")
    public ValidationResult__1 .Status getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(ValidationResult__1 .Status status) {
        this.status = status;
    }

    @JsonProperty("errorMessage")
    public String getErrorMessage() {
        return errorMessage;
    }

    @JsonProperty("errorMessage")
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public enum Status {

        OK("OK"),
        KO("KO");
        private final String value;
        private final static Map<String, ValidationResult__1 .Status> CONSTANTS = new HashMap<String, ValidationResult__1 .Status>();

        static {
            for (ValidationResult__1 .Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static ValidationResult__1 .Status fromValue(String value) {
            ValidationResult__1 .Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
