package be.cytomine.dto;

import lombok.Data;

@Data
public class AuthInformation {

    private Boolean admin;

    private Boolean user;

    private Boolean guest;

    private Boolean adminByNow;

    private Boolean userByNow;

    private Boolean guestByNow;
}
