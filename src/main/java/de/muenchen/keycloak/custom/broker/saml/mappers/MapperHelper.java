package de.muenchen.keycloak.custom.broker.saml.mappers;

import java.util.Base64;

/**
 *
 * @author rowe42
 */
public class MapperHelper {

    public static String extractIdFromBPK(String bPK) {
        if (bPK == null) {
            return "";
        }

        byte[] decodedBytes = Base64.getDecoder().decode(bPK);
        if (decodedBytes == null) {
            return "";
        }

        String decodedString = new String(decodedBytes);
        String[] splitDecodedString = decodedString.split("::");
        if (splitDecodedString.length > 2) {
            return splitDecodedString[2];
        } else return "";
    }

}
