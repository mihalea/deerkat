CREATE TABLE settings (
  property VARCHAR(255) PRIMARY KEY,
  value VARCHAR(255) NOT NULL
);

INSERT INTO settings (property, value) VALUES
  ('dbVersion', '2'),
  ('autoSort', 'true');