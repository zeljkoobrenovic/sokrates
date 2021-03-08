package nl.obren.sokrates.sourcecode.lang.adabasnatural;

class AdabasExamples {
    public static final String CONTENT = "* >Natural Source Header 000000\n" +
            "* :Mode S\n" +
            "* :CP\n" +
            "* <Natural Source Header\n" +
            "* MAP2: PROTOTYPE VERSION 820 --- CREATED BY ONE 8.3.8 ---\n" +
            "* INPUT USING MAP 'XXXXXXXX'\n" +
            "*     #CR-ED #CR-ET #CR-FROMH #CR-ID #CR-ID-CONTROL #CR-ID-FIND #CR-P1W #CR-P2W\n" +
            "*     #CR-P3W #CR-SD #CR-ST #CR-STATUS #CR-TOH #CR-YACHT-NAME\n" +
            "DEFINE DATA PARAMETER\n" +
            "1 #CR-ED (A013)\n" +
            "1 #CR-ET (A007)\n" +
            "1 #CR-FROMH (A020)\n" +
            "1 #CR-ID (N08.0)\n" +
            "1  #CR-ID-CONTROL (C)\n" +
            "1 #CR-ID-FIND (N08.0)\n" +
            "1 #CR-P1W (A020)\n" +
            "1 #CR-P2W (A020)\n" +
            "1 #CR-P3W (A020)\n" +
            "1 #CR-SD (A013)\n" +
            "1 #CR-ST (A007)\n" +
            "1 #CR-STATUS (A020)\n" +
            "1 #CR-TOH (A020)\n" +
            "1 #CR-YACHT-NAME (A020)\n" +
            "END-DEFINE\n" +
            "FIND NCYACHT YACHT-ID = NCCRUISE.ID-YACH\n" +
            "FORMAT PS=024 LS=080 ZP=OFF SG=OFF KD=OFF IP=OFF\n" +
            "* MAP2: MAP PROFILES *****************************        200***********\n" +
            "* .TTAAAMMOO   D I D I N D I D I        ?_)^&:+(   'NCDEMAPH'          *\n" +
            "* 024079        Y0NNUCN             X        01 SYSPROF NR             *\n" +
            "************************************************************************\n" +
            "INPUT          (     IP=OFF                                           /*\n" +
            "                                                                       )\n" +
            "/\n" +
            " 002T *USER /*.02S008 A008 .\n" +
            "      (AD=OD HE='NCDEMAPH' )\n" +
            " 024T 'Natural Class - Cruise Planning'(U)\n" +
            " 071T *TIMX /*.01S008 T    .\n" +
            "      (AD=OD HE='NCDEMAPH' )\n" +
            "/\n" +
            " 001T '_' (079)\n" +
            "/\n" +
            "/\n" +
            " 018T #CR-ID /*.99U008 N08.0.\n" +
            "      (AD=MIZ CV=#CR-ID-CONTROL HE='NCDEMAPH' )\n" +
            " 040T #CR-YACHT-NAME /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'Cruise-id'(C)\n" +
            " 040T 'Yacht-Name'(C)\n" +
            "/\n" +
            " 018T #CR-FROMH /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 040T #CR-TOH /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'from Harbor'(C)\n" +
            " 040T 'to Harbor'(C)\n" +
            "/\n" +
            " 018T #CR-SD /*.99U013 A013 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 031T #CR-ST /*.99U007 A007 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 040T #CR-ED /*.99U013 A013 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 053T #CR-ET /*.99U007 A007 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'starting'(C)\n" +
            " 040T 'ending'(C)\n" +
            "/\n" +
            "/\n" +
            " 018T #CR-STATUS /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 040T #CR-P1W /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'Status'(C)\n" +
            " 040T 'price one week'(C)\n" +
            "/\n" +
            " 040T #CR-P2W /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 040T 'price two weeks'(C)\n" +
            "/\n" +
            " 040T #CR-P3W /*.99U020 A020 .\n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 040T 'price three weeks'(C)\n" +
            "/\n" +
            "/\n" +
            " 018T 'Select Cruise by Id:'\n" +
            " 040T #CR-ID-FIND /*.99U008 N08.0.\n" +
            "      (AD=MDLTE HE='NCDECIDH',#CR-ID-FIND )\n" +
            "/\n" +
            " 001T '_' (079)\n" +
            "/\n" +
            " 002T 'F1-Help  F2-Show Cruise  F3-Stop Program'\n" +
            "/\n" +
            "/\n" +
            "/\n" +
            "* MAP2: VALIDATION *****************************************************\n" +
            "RULEVAR F00#CR-ID-FIND                                                      \n" +
            "INCDIC                                 ;\n" +
            "IF & = 0 AND *PF-KEY NE 'PF3'\n" +
            "  REINPUT 'Please enter a valid cruise id.'\n" +
            "  MARK *&\n" +
            "END-IF\n" +
            "* MAP2: END OF MAP *****************************************************\n" +
            "\n" +
            "DECIDE ON FIRST VALUE OF *PF-KEY\n" +
            "*\n" +
            "  VALUE 'PF2', 'ENTR'\n" +
            "    DECIDE ON FIRST VALUE #MENU-SELECTION\n" +
            "    VALUE '1'\n" +
            "      STACK TOP COMMAND 'NCMENUP'\n" +
            "      STACK TOP COMMAND 'NCINMAPP'\n" +
            "    VALUE '2'\n" +
            "      STACK TOP COMMAND 'NCMENUP'\n" +
            "      STACK TOP COMMAND 'NCATENDP'\n" +
            "    VALUE '3'\n" +
            "      TERMINATE\n" +
            "    NONE REINPUT 'Sorry - Selection not available'\n" +
            "    END-DECIDE\n" +
            "  VALUE 'PF3'\n" +
            "    TERMINATE\n" +
            "  NONE REINPUT 'Sorry - Function key not allocated'\n" +
            "END-DECIDE\n" +
            "END\n";
    public static final String CLEANED = "DEFINE DATA PARAMETER\n" +
            "1 #CR-ED (A013)\n" +
            "1 #CR-ET (A007)\n" +
            "1 #CR-FROMH (A020)\n" +
            "1 #CR-ID (N08.0)\n" +
            "1  #CR-ID-CONTROL (C)\n" +
            "1 #CR-ID-FIND (N08.0)\n" +
            "1 #CR-P1W (A020)\n" +
            "1 #CR-P2W (A020)\n" +
            "1 #CR-P3W (A020)\n" +
            "1 #CR-SD (A013)\n" +
            "1 #CR-ST (A007)\n" +
            "1 #CR-STATUS (A020)\n" +
            "1 #CR-TOH (A020)\n" +
            "1 #CR-YACHT-NAME (A020)\n" +
            "END-DEFINE\n" +
            "FIND NCYACHT YACHT-ID = NCCRUISE.ID-YACH\n" +
            "FORMAT PS=024 LS=080 ZP=OFF SG=OFF KD=OFF IP=OFF\n" +
            "INPUT          (     IP=OFF                                           \n" +
            "                                                                       )\n" +
            "/\n" +
            " 002T *USER \n" +
            "      (AD=OD HE='NCDEMAPH' )\n" +
            " 024T 'Natural Class - Cruise Planning'(U)\n" +
            " 071T *TIMX \n" +
            "      (AD=OD HE='NCDEMAPH' )\n" +
            "/\n" +
            " 001T '_' (079)\n" +
            "/\n" +
            "/\n" +
            " 018T #CR-ID \n" +
            "      (AD=MIZ CV=#CR-ID-CONTROL HE='NCDEMAPH' )\n" +
            " 040T #CR-YACHT-NAME \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'Cruise-id'(C)\n" +
            " 040T 'Yacht-Name'(C)\n" +
            "/\n" +
            " 018T #CR-FROMH \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 040T #CR-TOH \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'from Harbor'(C)\n" +
            " 040T 'to Harbor'(C)\n" +
            "/\n" +
            " 018T #CR-SD \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 031T #CR-ST \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 040T #CR-ED \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 053T #CR-ET \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'starting'(C)\n" +
            " 040T 'ending'(C)\n" +
            "/\n" +
            "/\n" +
            " 018T #CR-STATUS \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            " 040T #CR-P1W \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 018T 'Status'(C)\n" +
            " 040T 'price one week'(C)\n" +
            "/\n" +
            " 040T #CR-P2W \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 040T 'price two weeks'(C)\n" +
            "/\n" +
            " 040T #CR-P3W \n" +
            "      (AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            " 040T 'price three weeks'(C)\n" +
            "/\n" +
            "/\n" +
            " 018T 'Select Cruise by Id:'\n" +
            " 040T #CR-ID-FIND \n" +
            "      (AD=MDLTE HE='NCDECIDH',#CR-ID-FIND )\n" +
            "/\n" +
            " 001T '_' (079)\n" +
            "/\n" +
            " 002T 'F1-Help  F2-Show Cruise  F3-Stop Program'\n" +
            "/\n" +
            "/\n" +
            "/\n" +
            "RULEVAR F00#CR-ID-FIND                                                      \n" +
            "INCDIC                                 ;\n" +
            "IF & = 0 AND *PF-KEY NE 'PF3'\n" +
            "  REINPUT 'Please enter a valid cruise id.'\n" +
            "  MARK *&\n" +
            "END-IF\n" +
            "DECIDE ON FIRST VALUE OF *PF-KEY\n" +
            "  VALUE 'PF2', 'ENTR'\n" +
            "    DECIDE ON FIRST VALUE #MENU-SELECTION\n" +
            "    VALUE '1'\n" +
            "      STACK TOP COMMAND 'NCMENUP'\n" +
            "      STACK TOP COMMAND 'NCINMAPP'\n" +
            "    VALUE '2'\n" +
            "      STACK TOP COMMAND 'NCMENUP'\n" +
            "      STACK TOP COMMAND 'NCATENDP'\n" +
            "    VALUE '3'\n" +
            "      TERMINATE\n" +
            "    NONE REINPUT 'Sorry - Selection not available'\n" +
            "    END-DECIDE\n" +
            "  VALUE 'PF3'\n" +
            "    TERMINATE\n" +
            "  NONE REINPUT 'Sorry - Function key not allocated'\n" +
            "END-DECIDE\n" +
            "END";
    public static final String CLEANED_FOR_DUPLICATION = "DEFINE DATA PARAMETER\n" +
            "1 #CR-ED (A013)\n" +
            "1 #CR-ET (A007)\n" +
            "1 #CR-FROMH (A020)\n" +
            "1 #CR-ID (N08.0)\n" +
            "1 #CR-ID-CONTROL (C)\n" +
            "1 #CR-ID-FIND (N08.0)\n" +
            "1 #CR-P1W (A020)\n" +
            "1 #CR-P2W (A020)\n" +
            "1 #CR-P3W (A020)\n" +
            "1 #CR-SD (A013)\n" +
            "1 #CR-ST (A007)\n" +
            "1 #CR-STATUS (A020)\n" +
            "1 #CR-TOH (A020)\n" +
            "1 #CR-YACHT-NAME (A020)\n" +
            "END-DEFINE\n" +
            "FIND NCYACHT YACHT-ID = NCCRUISE.ID-YACH\n" +
            "FORMAT PS=024 LS=080 ZP=OFF SG=OFF KD=OFF IP=OFF\n" +
            "INPUT ( IP=OFF\n" +
            ")\n" +
            "/\n" +
            "002T *USER\n" +
            "(AD=OD HE='NCDEMAPH' )\n" +
            "024T 'Natural Class - Cruise Planning'(U)\n" +
            "071T *TIMX\n" +
            "(AD=OD HE='NCDEMAPH' )\n" +
            "/\n" +
            "001T '_' (079)\n" +
            "/\n" +
            "/\n" +
            "018T #CR-ID\n" +
            "(AD=MIZ CV=#CR-ID-CONTROL HE='NCDEMAPH' )\n" +
            "040T #CR-YACHT-NAME\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            "018T 'Cruise-id'(C)\n" +
            "040T 'Yacht-Name'(C)\n" +
            "/\n" +
            "018T #CR-FROMH\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "040T #CR-TOH\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            "018T 'from Harbor'(C)\n" +
            "040T 'to Harbor'(C)\n" +
            "/\n" +
            "018T #CR-SD\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "031T #CR-ST\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "040T #CR-ED\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "053T #CR-ET\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            "018T 'starting'(C)\n" +
            "040T 'ending'(C)\n" +
            "/\n" +
            "/\n" +
            "018T #CR-STATUS\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "040T #CR-P1W\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            "018T 'Status'(C)\n" +
            "040T 'price one week'(C)\n" +
            "/\n" +
            "040T #CR-P2W\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            "040T 'price two weeks'(C)\n" +
            "/\n" +
            "040T #CR-P3W\n" +
            "(AD=MIZ HE='NCDEMAPH' )\n" +
            "/\n" +
            "040T 'price three weeks'(C)\n" +
            "/\n" +
            "/\n" +
            "018T 'Select Cruise by Id:'\n" +
            "040T #CR-ID-FIND\n" +
            "(AD=MDLTE HE='NCDECIDH',#CR-ID-FIND )\n" +
            "/\n" +
            "001T '_' (079)\n" +
            "/\n" +
            "002T 'F1-Help F2-Show Cruise F3-Stop Program'\n" +
            "/\n" +
            "/\n" +
            "/\n" +
            "RULEVAR F00#CR-ID-FIND\n" +
            "INCDIC ;\n" +
            "IF & = 0 AND *PF-KEY NE 'PF3'\n" +
            "REINPUT 'Please enter a valid cruise id.'\n" +
            "MARK *&\n" +
            "END-IF\n" +
            "DECIDE ON FIRST VALUE OF *PF-KEY\n" +
            "VALUE 'PF2', 'ENTR'\n" +
            "DECIDE ON FIRST VALUE #MENU-SELECTION\n" +
            "VALUE '1'\n" +
            "STACK TOP COMMAND 'NCMENUP'\n" +
            "STACK TOP COMMAND 'NCINMAPP'\n" +
            "VALUE '2'\n" +
            "STACK TOP COMMAND 'NCMENUP'\n" +
            "STACK TOP COMMAND 'NCATENDP'\n" +
            "VALUE '3'\n" +
            "TERMINATE\n" +
            "NONE REINPUT 'Sorry - Selection not available'\n" +
            "END-DECIDE\n" +
            "VALUE 'PF3'\n" +
            "TERMINATE\n" +
            "NONE REINPUT 'Sorry - Function key not allocated'\n" +
            "END-DECIDE\n" +
            "END";
}