import unittest
from clean_install_sql import clean, statements


class CleanInstallSqlTest(unittest.TestCase):
    def test_literals_comments_and_trailing_whitespace_do_not_break_statement_boundaries(self):
        source = "-- comment; ignored\nSELECT 'a;b', 'it''s;ok';\n/* ; */ SELECT 2;\n"
        parts = list(statements(source))
        self.assertEqual(''.join(parts), source)
        self.assertEqual(len(parts), 3)

    def test_only_admin_identity_survives_while_schema_and_roles_remain(self):
        source = """CREATE TABLE sys_notice(id bigint);
INSERT INTO `sys_user` (id,username) VALUES
(1,'admin'),(42,'sample');
INSERT INTO `sys_user_role` (id,user_id,role_id) VALUES
(1,1,1),(2,42,3);
INSERT INTO `sys_dept` (id,name) VALUES
(1,'root'),(2,'example');
INSERT INTO `sys_notice` (id,content) VALUES
(1,'example; notice');
INSERT INTO `sys_role` (id,name) VALUES (3,'general');
"""
        actual = clean(source)
        self.assertIn('CREATE TABLE sys_notice', actual)
        self.assertIn("(1,'admin')", actual)
        self.assertNotIn('sample', actual)
        self.assertNotIn('example', actual)
        self.assertIn("(3,'general')", actual)
        self.assertEqual(clean(actual), actual)


if __name__ == '__main__': unittest.main()
