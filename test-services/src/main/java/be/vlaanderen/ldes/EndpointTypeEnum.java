package be.vlaanderen.ldes;

import java.util.Objects;

/**
 * The type of endpoint view of an LDES server.
 */
public enum EndpointTypeEnum {

    BY_LOCATION("byLocation"),
    BY_TIME("byTime"),
    BY_NAME("byName"),
    BY_PAGES("byPages");

    private final String type;

    /**
     * Constructor.
     *
     * @param type A string representation of the endpoint view.
     */
    EndpointTypeEnum(String type) {
        this.type = type;
    }

    /**
     * @return The endpoint view type.
     */
    public String getType() {
        return type;
    }

    /**
     * Parse the endpoint type from the provided string representation.
     *
     * @param type The type to parse.
     * @return The endpoint type.
     */
    public static EndpointTypeEnum fromType(String type) {
        Objects.requireNonNull(type, "The type of endpoint must be provided.");
        if (type.equals(BY_LOCATION.getType())) {
            return BY_LOCATION;
        } else if (type.equals(BY_NAME.getType())) {
            return BY_NAME;
        } else if (type.equals(BY_TIME.getType())) {
            return BY_TIME;
        } else if (type.equals(BY_PAGES.getType())) {
            return BY_PAGES;
        } else {
            throw new IllegalArgumentException(String.format("Invalid endpoint type [%s]", type));
        }
    }

}
