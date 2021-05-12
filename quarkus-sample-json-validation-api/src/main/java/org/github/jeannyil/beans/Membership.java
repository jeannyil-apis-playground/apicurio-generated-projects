
package org.github.jeannyil.beans;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Membership data 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "requestType",
    "requestID",
    "memberID",
    "status",
    "enrolmentDate",
    "changedBy",
    "forcedLevelCode",
    "vipOnInvitation",
    "startDate",
    "endDate"
})
public class Membership {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestType")
    private String requestType;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestID")
    private Integer requestID;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("memberID")
    private Integer memberID;
    @JsonProperty("status")
    private Membership.Status status;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("enrolmentDate")
    private String enrolmentDate;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changedBy")
    private String changedBy;
    @JsonProperty("forcedLevelCode")
    private String forcedLevelCode;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("vipOnInvitation")
    private Membership.VipOnInvitation vipOnInvitation;
    @JsonProperty("startDate")
    private String startDate;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("endDate")
    private String endDate;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestType")
    public String getRequestType() {
        return requestType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestType")
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestID")
    public Integer getRequestID() {
        return requestID;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestID")
    public void setRequestID(Integer requestID) {
        this.requestID = requestID;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("memberID")
    public Integer getMemberID() {
        return memberID;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("memberID")
    public void setMemberID(Integer memberID) {
        this.memberID = memberID;
    }

    @JsonProperty("status")
    public Membership.Status getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(Membership.Status status) {
        this.status = status;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("enrolmentDate")
    public String getEnrolmentDate() {
        return enrolmentDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("enrolmentDate")
    public void setEnrolmentDate(String enrolmentDate) {
        this.enrolmentDate = enrolmentDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changedBy")
    public String getChangedBy() {
        return changedBy;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changedBy")
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    @JsonProperty("forcedLevelCode")
    public String getForcedLevelCode() {
        return forcedLevelCode;
    }

    @JsonProperty("forcedLevelCode")
    public void setForcedLevelCode(String forcedLevelCode) {
        this.forcedLevelCode = forcedLevelCode;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("vipOnInvitation")
    public Membership.VipOnInvitation getVipOnInvitation() {
        return vipOnInvitation;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("vipOnInvitation")
    public void setVipOnInvitation(Membership.VipOnInvitation vipOnInvitation) {
        this.vipOnInvitation = vipOnInvitation;
    }

    @JsonProperty("startDate")
    public String getStartDate() {
        return startDate;
    }

    @JsonProperty("startDate")
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("endDate")
    public String getEndDate() {
        return endDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("endDate")
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public enum Status {

        A("A"),
        B("B"),
        C("C");
        private final String value;
        private final static Map<String, Membership.Status> CONSTANTS = new HashMap<String, Membership.Status>();

        static {
            for (Membership.Status c: values()) {
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
        public static Membership.Status fromValue(String value) {
            Membership.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum VipOnInvitation {

        N("N"),
        Y("Y");
        private final String value;
        private final static Map<String, Membership.VipOnInvitation> CONSTANTS = new HashMap<String, Membership.VipOnInvitation>();

        static {
            for (Membership.VipOnInvitation c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private VipOnInvitation(String value) {
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
        public static Membership.VipOnInvitation fromValue(String value) {
            Membership.VipOnInvitation constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
