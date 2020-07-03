create table gupshup_message(
	id BIGSERIAL PRIMARY KEY NOT NULL,
	phone_no VARCHAR(15) NOT NULL,
	updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
	message VARCHAR(10000) NOT NULL,
	is_last_message boolean not null
);


CREATE INDEX index_phone_no ON gupshup_message(phone_no);

create table gupshup_state(
	id BIGSERIAL PRIMARY KEY NOT NULL,
	phone_no VARCHAR(15) NOT NULL,
	state text,
	previous_path VARCHAR(100),
	bot_form_name VARCHAR(20),
	updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX index_state_phone_no ON gupshup_state(phone_no);
