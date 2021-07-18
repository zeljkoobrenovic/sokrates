package nl.obren.sokrates.sourcecode.contributors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContributorTest {

    @Test
    void isRookieAtDate() {
        Contributor c = new Contributor();

        c.setFirstCommitDate("2018-04-05");

        assertTrue(c.isRookieAtDate("2017-04-05"));
        assertTrue(c.isRookieAtDate("2018-04-04"));
        assertTrue(c.isRookieAtDate("2018-04-05"));
        assertTrue(c.isRookieAtDate("2018-04-06"));
        assertTrue(c.isRookieAtDate("2019-04-05"));
        assertFalse(c.isRookieAtDate("2019-04-06"));
        assertFalse(c.isRookieAtDate("2019-07-06"));
        assertFalse(c.isRookieAtDate("2020-02-15"));
        assertFalse(c.isRookieAtDate("2021-03-11"));
    }
}