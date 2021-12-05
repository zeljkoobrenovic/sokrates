package nl.obren.sokrates.sourcecode.lang.plsql;

public class PlSqlExamples {
    public static final String CONTENT_1 = "DECLARE\n" +
            "   -- variable declaration\n" +
            "   name varchar2(20); \n" +
            "   company varchar2(30); \n" +
            "   introduction clob; \n" +
            "   choice char(1); \n" +
            "   \n" +
            "BEGIN \n" +
            "\n" +
            "   /* \n" +
            "   *  PL/SQL executable statement(s) \n" +
            "   */\n" +
            "   name := 'John Smith'; \n" +
            "   company := 'Infotech'; \n" +
            "   introduction := ' Hello! I''m John Smith from Infotech.'; \n" +
            "   choice := 'y'; \n" +
            "   IF choice = 'y' THEN \n" +
            "      dbms_output.put_line(name); \n" +
            "      dbms_output.put_line(company); \n" +
            "      dbms_output.put_line(introduction); \n" +
            "   END IF; \n" +
            "   \n" +
            "   -- end of block\n" +
            "END;";
    public static final String CONTENT_1_CLEANED = "DECLARE\n" +
            "   name varchar2(20); \n" +
            "   company varchar2(30); \n" +
            "   introduction clob; \n" +
            "   choice char(1); \n" +
            "BEGIN \n" +
            "   name := 'John Smith'; \n" +
            "   company := 'Infotech'; \n" +
            "   introduction := ' Hello! I''m John Smith from Infotech.'; \n" +
            "   choice := 'y'; \n" +
            "   IF choice = 'y' THEN \n" +
            "      dbms_output.put_line(name); \n" +
            "      dbms_output.put_line(company); \n" +
            "      dbms_output.put_line(introduction); \n" +
            "   END IF; \n" +
            "END;";
    public static final String CONTENT_2 = "DECLARE\n" +
            "   -- variable declaration\n" +
            "   c_id customers.id%type := &cc_id;\n" +
            "   c_name customerS.Name%type;\n" +
            "   c_addr customers.address%type;\n" +
            "   -- user defined exception\n" +
            "   ex_invalid_id  EXCEPTION;\n" +
            "BEGIN\n" +
            "   IF c_id <= 0 THEN\n" +
            "      RAISE ex_invalid_id;\n" +
            "   ELSE\n" +
            "      SELECT  name, address INTO  c_name, c_addr\n" +
            "      FROM customers\n" +
            "      WHERE id = c_id;\n" +
            "      DBMS_OUTPUT.PUT_LINE ('Name: '||  c_name);\n" +
            "      DBMS_OUTPUT.PUT_LINE ('Address: ' || c_addr);\n" +
            "   END IF;\n" +
            "\n" +
            "EXCEPTION\n" +
            "    /*\n" +
            "    *  PL/SQL exception handling\n" +
            "    */\n" +
            "   WHEN ex_invalid_id THEN\n" +
            "      dbms_output.put_line('ID must be greater than zero!');\n" +
            "   WHEN no_data_found THEN\n" +
            "      dbms_output.put_line('No such customer!');\n" +
            "   WHEN others THEN\n" +
            "      dbms_output.put_line('Error!');\n" +
            "END;";
    public static final String CONTENT_2_CLEANED_FOR_DUPLICATION = "DECLARE\n" +
            "c_id customers.id%type := &cc_id;\n" +
            "c_name customerS.Name%type;\n" +
            "c_addr customers.address%type;\n" +
            "ex_invalid_id EXCEPTION;\n" +
            "BEGIN\n" +
            "IF c_id <= 0 THEN\n" +
            "RAISE ex_invalid_id;\n" +
            "ELSE\n" +
            "SELECT name, address INTO c_name, c_addr\n" +
            "FROM customers\n" +
            "WHERE id = c_id;\n" +
            "DBMS_OUTPUT.PUT_LINE ('Name: '|| c_name);\n" +
            "DBMS_OUTPUT.PUT_LINE ('Address: ' || c_addr);\n" +
            "END IF;\n" +
            "EXCEPTION\n" +
            "WHEN ex_invalid_id THEN\n" +
            "dbms_output.put_line('ID must be greater than zero!');\n" +
            "WHEN no_data_found THEN\n" +
            "dbms_output.put_line('No such customer!');\n" +
            "WHEN others THEN\n" +
            "dbms_output.put_line('Error!');\n" +
            "END;";
    public static final String CONTENT_3 = "CREATE PROCEDURE create_email_address ( -- Procedure heading part begins\n" +
            "    name1 VARCHAR2,\n" +
            "    name2 VARCHAR2,\n" +
            "    company VARCHAR2,\n" +
            "    email OUT VARCHAR2\n" +
            ") -- Procedure heading part ends\n" +
            "AS\n" +
            "-- Declarative part begins (optional)\n" +
            "error_message VARCHAR2(30) := 'Email address is too long.';\n" +
            "BEGIN -- Executable part begins (mandatory)\n" +
            "    email := name1 || '.' || name2 || '@' || company;\n" +
            "EXCEPTION -- Exception-handling part begins (optional)\n" +
            "WHEN VALUE_ERROR THEN\n" +
            "    DBMS_OUTPUT.PUT_LINE(error_message);\n" +
            "END create_email_address;";
    public static final String CONTENT_4 = "CREATE OR REPLACE PACKAGE aa_pkg AUTHID DEFINER IS\n" +
            "  TYPE aa_type IS TABLE OF INTEGER INDEX BY VARCHAR2(15);\n" +
            "END;\n" +
            "/\n" +
            "CREATE OR REPLACE PROCEDURE print_aa (\n" +
            "  aa aa_pkg.aa_type\n" +
            ") AUTHID DEFINER IS\n" +
            "  i  VARCHAR2(15);\n" +
            "BEGIN\n" +
            "  i := aa.FIRST;\n" +
            " \n" +
            "  WHILE i IS NOT NULL LOOP\n" +
            "    DBMS_OUTPUT.PUT_LINE (aa(i) || '  ' || i);\n" +
            "    i := aa.NEXT(i);\n" +
            "  END LOOP;\n" +
            "END;\n" +
            "/\n" +
            "DECLARE\n" +
            "  aa_var  aa_pkg.aa_type;\n" +
            "BEGIN\n" +
            "  aa_var('zero') := 0;\n" +
            "  aa_var('one') := 1;\n" +
            "  aa_var('two') := 2;\n" +
            "  print_aa(aa_var);\n" +
            "END;\n" +
            "/";
    public static final String CONTENT_5 = "CREATE OR REPLACE PACKAGE c_package AS\n" +
            "   -- Adds a customer\n" +
            "   PROCEDURE addCustomer(c_id   customers.id%type,\n" +
            "   c_name customers.Name%type,\n" +
            "   c_age  customers.age%type,\n" +
            "   c_addr customers.address%type,\n" +
            "   c_sal  customers.salary%type);\n" +
            "   \n" +
            "   -- Removes a customer\n" +
            "   PROCEDURE delCustomer(c_id  customers.id%TYPE);\n" +
            "   --Lists all customers\n" +
            "   PROCEDURE listCustomer;\n" +
            "  \n" +
            "END c_package;\n" +
            "/\n" +
            "CREATE OR REPLACE PACKAGE BODY c_package AS\n" +
            "   PROCEDURE addCustomer(c_id  customers.id%type,\n" +
            "      c_name customers.Name%type,\n" +
            "      c_age  customers.age%type,\n" +
            "      c_addr  customers.address%type,\n" +
            "      c_sal   customers.salary%type)\n" +
            "   IS\n" +
            "   BEGIN\n" +
            "      INSERT INTO customers (id,name,age,address,salary)\n" +
            "         VALUES(c_id, c_name, c_age, c_addr, c_sal);\n" +
            "   END addCustomer;\n" +
            "   \n" +
            "   PROCEDURE delCustomer(c_id   customers.id%type) IS\n" +
            "   BEGIN\n" +
            "      DELETE FROM customers\n" +
            "      WHERE id = c_id;\n" +
            "   END delCustomer;\n" +
            "   \n" +
            "   PROCEDURE listCustomer IS\n" +
            "   CURSOR c_customers is\n" +
            "      SELECT  name FROM customers;\n" +
            "   TYPE c_list is TABLE OF customers.Name%type;\n" +
            "   name_list c_list := c_list();\n" +
            "   counter integer :=0;\n" +
            "   BEGIN\n" +
            "      FOR n IN c_customers LOOP\n" +
            "      counter := counter +1;\n" +
            "      name_list.extend;\n" +
            "      name_list(counter) := n.name;\n" +
            "      dbms_output.put_line('Customer(' ||counter|| ')'||name_list(counter));\n" +
            "      END LOOP;\n" +
            "   END listCustomer;\n" +
            "\n" +
            "END c_package;\n" +
            "/\n";
    public static final String CONTENT_6 =  "DECLARE\n" +
            "   code customers.id%type:= 8;\n" +
            "BEGIN\n" +
            "   c_package.addCustomer(7, 'Rajnish', 25, 'Chennai', 3500);\n" +
            "   c_package.addCustomer(8, 'Subham', 32, 'Delhi', 7500);\n" +
            "   c_package.listCustomer;\n" +
            "   c_package.delCustomer(code);\n" +
            "   c_package.listCustomer;\n" +
            "END;";
    public static final String CONTENT_7 = "CREATE PACKAGE emp_bonus AS\n" +
            "  PROCEDURE calc_bonus (date_hired employees.hire_date%TYPE);\n" +
            "END emp_bonus;\n" +
            "/\n" +
            "CREATE OR REPLACE PACKAGE BODY emp_bonus AS\n" +
            "  PROCEDURE calc_bonus\n" +
            "    (date_hired employees.hire_date%TYPE) IS\n" +
            "  BEGIN\n" +
            "    DBMS_OUTPUT.PUT_LINE\n" +
            "      ('Employees hired on ' || date_hired || ' get bonus.');\n" +
            "  END;\n" +
            "END emp_bonus;\n" +
            "/";
    public static final String CONTENT_8 = "CREATE OR REPLACE PROCEDURE fetch_from_cursor IS\n" +
            "  v_name  people.name%TYPE;\n" +
            "BEGIN\n" +
            "  IF sr_pkg.c%ISOPEN THEN\n" +
            "    emp_bonus.calc_bonus;\n" +
            "  ELSE\n" +
            "    emp_bonus.PUT_LINE('Cursor is closed; opening now.');\n" +
            "    OPEN sr_pkg.c;\n" +
            "  END IF;\n" +
            " \n" +
            "  FETCH sr_pkg.c INTO v_name;\n" +
            "  c_package.delCustomer(code);\n" +
            " \n" +
            "  FETCH sr_pkg.c INTO v_name;\n" +
            "    c_package.PUT_LINE('Fetched: ' || v_name);\n" +
            "END fetch_from_cursor;";
}
