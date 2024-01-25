CREATE EXTENSION IF NOT EXISTS plv8;

CREATE or replace FUNCTION naksha_init_v8() returns void AS $BODY$
  plv8.__init = function () {
    ${here-naksha-lib-plv8.js}
  }
  plv8.__init();
$BODY$ LANGUAGE 'plv8' IMMUTABLE;

CREATE or replace FUNCTION evalToText(command text) returns text AS $BODY$
  return eval(command);
$BODY$ LANGUAGE 'plv8' IMMUTABLE;