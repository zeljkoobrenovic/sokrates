package nl.obren.sokrates.sourcecode.lang.abap;

class AbapExamples {
        public static final String CONTENT = "\"! <p class=\"shorttext synchronized\" lang=\"en\">Flight Data as Instance Methods</p>\n"+
        "CLASS zcl_oo_tutorial_3 DEFINITION\n"+
        "  PUBLIC\n"+
        "  FINAL\n"+
        "  CREATE PUBLIC .\n"+
        "\n"+
        "  PUBLIC SECTION.\n"+
        "    INTERFACES if_serializable_object.\n"+
        "    \"! <p class=\"shorttext synchronized\" lang=\"en\">Flight</p>\n"+
        "    DATA flight TYPE /dmo/flight.\"Comment in line\n"+
        "\n"+
        "    \"! <p class=\"shorttext synchronized\" lang=\"en\">CONSTRUCTOR</p>\n"+
        "    METHODS constructor\n"+
        "      IMPORTING\n"+
        "        !carrier_id    TYPE /dmo/carrier_id\n"+
        "        !connection_id TYPE /dmo/connection_id\n"+
        "        !flight_date   TYPE /dmo/flight_date.\n"+
        "\n"+
        "    \"! <p class=\"shorttext synchronized\" lang=\"en\">Get Booking Details</p>\n"+
        "    METHODS get_flight_details\n"+
        "      RETURNING VALUE(flight) TYPE /dmo/flight .\n"+
        "\n"+
        "    \"! <p class=\"shorttext synchronized\" lang=\"en\">Calculate Flight Price</p>\n"+
        "    METHODS calculate_flight_price\n"+
        "      EXPORTING\n"+
        "        !price         TYPE /dmo/flight_price\n"+
        "        !currency_code TYPE /dmo/currency_code.\n"+
        "  PROTECTED SECTION.\n"+
        " PRIVATE SECTION.\n"+
        "ENDCLASS.\n"+
        "\n"+
        "\n"+
        "\n"+
        "CLASS zcl_oo_tutorial_3 IMPLEMENTATION.\n"+
        "\n"+
        "\n"+
        "  METHOD calculate_flight_price.\n"+
        "\n"+
        "    price = me->flight-price.\n"+
        "    currency_code = me->flight-currency_code.\n"+
        "\n"+
        "    CASE me->flight-plane_type_id.\n"+
        "      WHEN \'747-400\'.\n"+
        "        price = price + 40.\n"+
        "      WHEN \'A310-300\'.\n"+
        "        price = price + 25.\n"+
        "      WHEN OTHERS.\n"+
        "        price = price + 10.\n"+
        "    ENDCASE.\n"+
        "  ENDMETHOD.\n"+
        "\n"+
        "***duplicate\n"+
        "  METHOD calculate_flight_price2.\n"+
        "\n"+
        "    price = me->flight-price.\n"+
        "    currency_code = me->flight-currency_code.\n"+
        "\n"+
        "    CASE me->flight-plane_type_id.\n"+
        "      WHEN \'747-400\'.\n"+
        "        price = price + 40.\n"+
        "      WHEN \'A310-300\'.\n"+
        "        price = price + 25.\n"+
        "      WHEN OTHERS.\n"+
        "        price = price + 10.\n"+
        "    ENDCASE.\n"+
        "  ENDMETHOD.\n"+
        "\n"+
        "\n"+
        "  METHOD constructor.\n"+
        "    SELECT SINGLE * FROM /dmo/flight\n"+
        "      WHERE carrier_id = @carrier_id\n"+
        "        AND connection_id = @connection_id\n"+
        "        AND flight_date = @flight_date\n"+
        "       INTO @flight.\n"+
        "  ENDMETHOD.\n"+
        "\n"+
        "\n"+
        "  METHOD get_flight_details.\n"+
        "    flight = me->flight.\n"+
        "  ENDMETHOD.\n"+
        "ENDCLASS.";
        public static final String CLEANED =         "CLASS zcl_oo_tutorial_3 DEFINITION\n"+
        "  PUBLIC\n"+
        "  FINAL\n"+
        "  CREATE PUBLIC .\n"+
        "  PUBLIC SECTION.\n"+
        "    INTERFACES if_serializable_object.\n"+
        "    DATA flight TYPE /dmo/flight.\n"+
        "    METHODS constructor\n"+
        "      IMPORTING\n"+
        "        !carrier_id    TYPE /dmo/carrier_id\n"+
        "        !connection_id TYPE /dmo/connection_id\n"+
        "        !flight_date   TYPE /dmo/flight_date.\n"+
        "    METHODS get_flight_details\n"+
        "      RETURNING VALUE(flight) TYPE /dmo/flight .\n"+
        "    METHODS calculate_flight_price\n"+
        "      EXPORTING\n"+
        "        !price         TYPE /dmo/flight_price\n"+
        "        !currency_code TYPE /dmo/currency_code.\n"+
        "  PROTECTED SECTION.\n"+
        " PRIVATE SECTION.\n"+
        "ENDCLASS.\n"+
        "CLASS zcl_oo_tutorial_3 IMPLEMENTATION.\n"+
        "  METHOD calculate_flight_price.\n"+
        "    price = me->flight-price.\n"+
        "    currency_code = me->flight-currency_code.\n"+
        "    CASE me->flight-plane_type_id.\n"+
        "      WHEN \'747-400\'.\n"+
        "        price = price + 40.\n"+
        "      WHEN \'A310-300\'.\n"+
        "        price = price + 25.\n"+
        "      WHEN OTHERS.\n"+
        "        price = price + 10.\n"+
        "    ENDCASE.\n"+
        "  ENDMETHOD.\n"+
        "  METHOD calculate_flight_price2.\n"+
        "    price = me->flight-price.\n"+
        "    currency_code = me->flight-currency_code.\n"+
        "    CASE me->flight-plane_type_id.\n"+
        "      WHEN \'747-400\'.\n"+
        "        price = price + 40.\n"+
        "      WHEN \'A310-300\'.\n"+
        "        price = price + 25.\n"+
        "      WHEN OTHERS.\n"+
        "        price = price + 10.\n"+
        "    ENDCASE.\n"+
        "  ENDMETHOD.\n"+
        "  METHOD constructor.\n"+
        "    SELECT SINGLE * FROM /dmo/flight\n"+
        "      WHERE carrier_id = @carrier_id\n"+
        "        AND connection_id = @connection_id\n"+
        "        AND flight_date = @flight_date\n"+
        "       INTO @flight.\n"+
        "  ENDMETHOD.\n"+
        "  METHOD get_flight_details.\n"+
        "    flight = me->flight.\n"+
        "  ENDMETHOD.\n"+
        "ENDCLASS.";
        public static final String CLEANED_FOR_DUPLICATION ="CLASS zcl_oo_tutorial_3 DEFINITION\n"+
        "  PUBLIC\n"+
        "  FINAL\n"+
        "  CREATE PUBLIC .\n"+
        "  PUBLIC SECTION.\n"+
        "    INTERFACES if_serializable_object.\n"+
        "    DATA flight TYPE /dmo/flight.\n"+
        "    METHODS constructor\n"+
        "      IMPORTING\n"+
        "        !carrier_id    TYPE /dmo/carrier_id\n"+
        "        !connection_id TYPE /dmo/connection_id\n"+
        "        !flight_date   TYPE /dmo/flight_date.\n"+
        "    METHODS get_flight_details\n"+
        "      RETURNING VALUE(flight) TYPE /dmo/flight .\n"+
        "    METHODS calculate_flight_price\n"+
        "      EXPORTING\n"+
        "        !price         TYPE /dmo/flight_price\n"+
        "        !currency_code TYPE /dmo/currency_code.\n"+
        "  PROTECTED SECTION.\n"+
        " PRIVATE SECTION.\n"+
        "ENDCLASS.\n"+
        "CLASS zcl_oo_tutorial_3 IMPLEMENTATION.\n"+
        "  METHOD calculate_flight_price.\n"+
        "    price = me->flight-price.\n"+
        "    currency_code = me->flight-currency_code.\n"+
        "    CASE me->flight-plane_type_id.\n"+
        "      WHEN \'747-400\'.\n"+
        "        price = price + 40.\n"+
        "      WHEN \'A310-300\'.\n"+
        "        price = price + 25.\n"+
        "      WHEN OTHERS.\n"+
        "        price = price + 10.\n"+
        "    ENDCASE.\n"+
        "  ENDMETHOD.\n"+
        "  METHOD calculate_flight_price2.\n"+
        "    price = me->flight-price.\n"+
        "    currency_code = me->flight-currency_code.\n"+
        "    CASE me->flight-plane_type_id.\n"+
        "      WHEN \'747-400\'.\n"+
        "        price = price + 40.\n"+
        "      WHEN \'A310-300\'.\n"+
        "        price = price + 25.\n"+
        "      WHEN OTHERS.\n"+
        "        price = price + 10.\n"+
        "    ENDCASE.\n"+
        "  ENDMETHOD.\n"+
        "  METHOD constructor.\n"+
        "    SELECT SINGLE * FROM /dmo/flight\n"+
        "      WHERE carrier_id = @carrier_id\n"+
        "        AND connection_id = @connection_id\n"+
        "        AND flight_date = @flight_date\n"+
        "       INTO @flight.\n"+
        "  ENDMETHOD.\n"+
        "  METHOD get_flight_details.\n"+
        "    flight = me->flight.\n"+
        "  ENDMETHOD.\n"+
        "ENDCLASS.";      
}