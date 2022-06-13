CREATE TABLE "status_vans"(
    "id" SERIAL PRIMARY KEY NOT NULL ,
    "code" CHARACTER VARYING(15) UNIQUE NOT NULL ,
    "full_name" CHARACTER VARYING(100) UNIQUE NOT NULL ,
    "short_name" CHARACTER VARYING(30) UNIQUE NOT NULL ,
    "created" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    "updated" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "van_types"(
    "id" SERIAL PRIMARY KEY NOT NULL ,
    "full_name" CHARACTER VARYING(50) UNIQUE NOT NULL ,
    "short_name" CHARACTER VARYING(20) UNIQUE NOT NULL ,
    "created" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    "updated" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "stations"(
    "id" SERIAL PRIMARY KEY NOT NULL ,
    "full_name" CHARACTER VARYING(100) UNIQUE NOT NULL ,
    "short_name" CHARACTER VARYING(40) UNIQUE NOT NULL ,
    "status" CHARACTER VARYING(20) NOT NULL ,
    "address" CHARACTER VARYING(25),
    "latitude" DOUBLE PRECISION ,
    "longitude" DOUBLE PRECISION ,
    "sequence" DOUBLE PRECISION ,
    "created" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    "updated" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "data"(
    "id" SERIAL PRIMARY KEY NOT NULL ,
    "number_van" CHARACTER VARYING (15) NOT NULL ,
    "code_of_the_property" CHARACTER VARYING (20) ,
    "last_station" CHARACTER VARYING (100) ,
    "current_station" CHARACTER VARYING(100) ,
    "status_van" CHARACTER VARYING (100) ,
    "year" DOUBLE PRECISION ,
    "date" CHARACTER VARYING (10) ,
    "time" CHARACTER VARYING (10) ,
    "year_date_time" TIMESTAMP ,
    "type_van" CHARACTER VARYING (50) ,
    "set_station" CHARACTER VARYING(100) ,
    "hour_for_passed_way" BIGINT ,
    "day_for_repair" BIGINT ,
    "index_train" CHARACTER VARYING(30) ,
    "created" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    "updated" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    CHECK("year">0)
);