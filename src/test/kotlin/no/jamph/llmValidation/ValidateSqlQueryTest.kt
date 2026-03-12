package no.jamph.llmValidation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateSqlQueryTest {

    // Valid SQL queries examples
    @Test
    fun `valid SELECT returns true`() {
        assertTrue(isSqlQueryValid("SELECT * FROM users"))
    }

    @Test
    fun `valid SELECT with WHERE returns true`() {
        assertTrue(isSqlQueryValid("SELECT name FROM users WHERE id = 1"))
    }

    @Test
    fun `valid with CTE returns true`() {
        assertTrue(isSqlQueryValid("WITH cte AS (SELECT id FROM users) SELECT * FROM cte"))
    }

    // Empty string

    @Test
    fun `empty string returns false`() {
        assertFalse(isSqlQueryValid(""))
    }

    @Test
    fun `blank string returns false`() {
        assertFalse(isSqlQueryValid("   "))
    }

    // Dangerous commands

    @Test
    fun `DROP TABLE returns false`() {
        assertFalse(isSqlQueryValid("DROP TABLE users"))
    }

    @Test
    fun `DELETE FROM returns false`() {
        assertFalse(isSqlQueryValid("DELETE FROM users WHERE id = 1"))
    }

    @Test
    fun `TRUNCATE returns false`() {
        assertFalse(isSqlQueryValid("TRUNCATE TABLE users"))
    }

    @Test
    fun `UPDATE returns false`() {
        assertFalse(isSqlQueryValid("UPDATE users SET name = 'test' WHERE ID = 1"))
    }

    @Test
    fun `INSERT returns false`() {
        assertFalse(isSqlQueryValid("INSERT INTO users (name) VALUES ('test')"))
    }

    @Test
    fun `CREATE TABLE returns false`() {
        assertFalse(isSqlQueryValid("CREATE TABLE test (id INT)"))
    }

    @Test
    fun `ALTER TABLE returns false`() {
        assertFalse(isSqlQueryValid("ALTER TABLE users ADD COLUMN age INT"))
    }

    @Test
    fun `MERGE returns false`() {
        assertFalse(isSqlQueryValid("MERGE INTO users USING source ON users.id = source.id"))
    }

    @Test 
    fun `REPLACE returns false`() {
        assertFalse(isSqlQueryValid("REPLACE INTO users (id, name) VALUES (1, 'test')"))
    }

    // Invalid syntax
    
    @Test
    fun `incomplete SELECT returns false`() {
        assertFalse(isSqlQueryValid("SELECT * FROM"))
    }

    @Test
    fun `random text returns false`() {
        assertFalse(isSqlQueryValid("this is not SQL"))
    }

    @Test
    fun `typo in SELECT returns false`() {
        assertFalse(isSqlQueryValid("SELECCT * FROM users"))
    }
}