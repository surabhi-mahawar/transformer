create table xMessage_state(
	id BIGSERIAL PRIMARY KEY NOT NULL,
	phone_no VARCHAR(15) NOT NULL,
	state text,
	previous_path VARCHAR(100),
	bot_form_name VARCHAR(20),
	updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX index_state_phone_no ON xMessage_state(phone_no);
