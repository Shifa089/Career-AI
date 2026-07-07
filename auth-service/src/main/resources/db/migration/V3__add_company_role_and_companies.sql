-- Employer / company HR support.

-- New grantable role for company accounts.
INSERT INTO roles (name) VALUES ('ROLE_COMPANY');

-- A company profile owned by the HR user who registered it.
CREATE TABLE companies (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name          VARCHAR(200) NOT NULL,
    website       VARCHAR(300),
    industry      VARCHAR(150),
    company_size  VARCHAR(50),
    description   TEXT,
    owner_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_companies_owner ON companies (owner_user_id);
