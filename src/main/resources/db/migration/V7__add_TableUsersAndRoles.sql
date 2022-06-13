CREATE TABLE "users"(
    "id" SERIAL PRIMARY KEY NOT NULL ,
    "name" CHARACTER VARYING (10) NOT NULL ,
    "surname" CHARACTER VARYING (20) NOT NULL ,
    "email" CHARACTER VARYING (30) UNIQUE NOT NULL ,
    "password" CHARACTER VARYING (255) NOT NULL ,
    "image_path" CHARACTER  VARYING (75)
);

CREATE TABLE "roles"(
    "id" SERIAL PRIMARY KEY NOT NULL ,
    "name" CHARACTER VARYING (20) NOT NULL
);

CREATE TABLE "roles_users"(
    "role_id" INT NOT NULL ,
    "user_id" INT NOT NULL ,

    CONSTRAINT "roles_users_role_id"
        FOREIGN KEY ("role_id")
            REFERENCES "roles"("id")
                ON UPDATE CASCADE ON DELETE SET NULL ,
    CONSTRAINT "roles_users_user_id"
        FOREIGN KEY ("user_id")
            REFERENCES "users"("id")
                ON UPDATE CASCADE ON DELETE CASCADE
);