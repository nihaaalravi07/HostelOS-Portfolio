-- HostelOS schema (PostgreSQL). Money is always NUMERIC(12,2).

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(30)  NOT NULL
        CHECK (role IN ('STUDENT','WARDEN','ADMIN','MESS_STAFF','HOUSEKEEPING_STAFF','MAINTENANCE_STAFF')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE residents (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL UNIQUE REFERENCES users(id),
    enrollment_no  VARCHAR(30)  NOT NULL UNIQUE,
    phone          VARCHAR(20),
    course         VARCHAR(60)  NOT NULL,
    year_of_study  INT          NOT NULL CHECK (year_of_study BETWEEN 1 AND 6),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','CHECKED_OUT'))
);

-- ---------- Accommodation ----------
CREATE TABLE hostels (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    hostel_id   BIGINT      NOT NULL REFERENCES hostels(id),
    floor_no    INT         NOT NULL CHECK (floor_no >= 0),
    room_number VARCHAR(10) NOT NULL,
    capacity    INT         NOT NULL CHECK (capacity BETWEEN 1 AND 6),
    UNIQUE (hostel_id, room_number)
);

CREATE TABLE beds (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT NOT NULL REFERENCES rooms(id),
    bed_number INT    NOT NULL,
    UNIQUE (room_id, bed_number)
);

CREATE TABLE allocations (
    id          BIGSERIAL PRIMARY KEY,
    resident_id BIGINT NOT NULL REFERENCES residents(id),
    bed_id      BIGINT NOT NULL REFERENCES beds(id),
    start_date  DATE   NOT NULL,
    end_date    DATE   -- NULL while the allocation is active; history is retained
);
-- A bed can have only one active occupant and a resident only one active bed.
CREATE UNIQUE INDEX uq_alloc_active_bed      ON allocations(bed_id)      WHERE end_date IS NULL;
CREATE UNIQUE INDEX uq_alloc_active_resident ON allocations(resident_id) WHERE end_date IS NULL;

-- ---------- Mess ----------
CREATE TABLE meals (
    id          BIGSERIAL PRIMARY KEY,
    meal_date   DATE          NOT NULL,
    category    VARCHAR(20)   NOT NULL CHECK (category IN ('BREAKFAST','LUNCH','SNACKS','DINNER')),
    menu_items  VARCHAR(400)  NOT NULL,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    cutoff_time TIMESTAMP     NOT NULL,
    available   BOOLEAN       NOT NULL DEFAULT TRUE,
    image_url   VARCHAR(400),
    UNIQUE (meal_date, category)
);

CREATE TABLE meal_bookings (
    id              BIGSERIAL PRIMARY KEY,
    meal_id         BIGINT      NOT NULL REFERENCES meals(id),
    resident_id     BIGINT      NOT NULL REFERENCES residents(id),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('BOOKED','CANCELLED','CONSUMED','MISSED')),
    booked_at       TIMESTAMP   NOT NULL,
    cancelled_at    TIMESTAMP,
    consumed_at     TIMESTAMP,
    confirmed_by    BIGINT REFERENCES users(id),
    charged_amount  NUMERIC(12,2),
    UNIQUE (meal_id, resident_id)
);
CREATE INDEX idx_bookings_resident ON meal_bookings(resident_id);

CREATE TABLE food_feedback (
    id             BIGSERIAL PRIMARY KEY,
    meal_id        BIGINT      NOT NULL REFERENCES meals(id),
    resident_id    BIGINT      NOT NULL REFERENCES residents(id),
    taste_rating   INT         NOT NULL CHECK (taste_rating BETWEEN 1 AND 5),
    hygiene_rating INT         NOT NULL CHECK (hygiene_rating BETWEEN 1 AND 5),
    comment        VARCHAR(500),
    created_at     TIMESTAMP   NOT NULL,
    UNIQUE (meal_id, resident_id)
);

-- ---------- Finance: append-only ledger ----------
CREATE TABLE ledger_entries (
    id            BIGSERIAL PRIMARY KEY,
    resident_id   BIGINT        NOT NULL REFERENCES residents(id),
    type          VARCHAR(20)   NOT NULL
        CHECK (type IN ('CHARGE','PAYMENT','REFUND','ADJUSTMENT_DEBIT','ADJUSTMENT_CREDIT')),
    amount        NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    description   VARCHAR(250)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    source_module VARCHAR(30)   NOT NULL,
    source_ref    VARCHAR(80)   NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED')),
    -- Duplicate protection: one ledger entry per (module, source transaction).
    CONSTRAINT uq_ledger_source UNIQUE (source_module, source_ref)
);
CREATE INDEX idx_ledger_resident ON ledger_entries(resident_id);

-- Completed financial transactions may never be edited or deleted; corrections are reversals/refunds.
CREATE FUNCTION ledger_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only: post a reversal instead';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_ledger_immutable BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_immutable();

-- ---------- Housekeeping ----------
CREATE TABLE cleaning_requests (
    id               BIGSERIAL PRIMARY KEY,
    resident_id      BIGINT       NOT NULL REFERENCES residents(id),
    room_id          BIGINT       NOT NULL REFERENCES rooms(id),
    room_label       VARCHAR(60)  NOT NULL,
    request_date     DATE         NOT NULL,
    slot             VARCHAR(20)  NOT NULL CHECK (slot IN ('MORNING','LATE_MORNING','AFTERNOON','EVENING')),
    instructions     VARCHAR(300),
    status           VARCHAR(20)  NOT NULL
        CHECK (status IN ('REQUESTED','SCHEDULED','ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    assigned_to      BIGINT REFERENCES users(id),
    completion_notes VARCHAR(300),
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);
-- No two live bookings for the same room, date and slot.
CREATE UNIQUE INDEX uq_cleaning_room_slot ON cleaning_requests(room_id, request_date, slot)
    WHERE status <> 'CANCELLED';

-- ---------- Maintenance ----------
CREATE TABLE maintenance_complaints (
    id               BIGSERIAL PRIMARY KEY,
    resident_id      BIGINT       NOT NULL REFERENCES residents(id),
    room_id          BIGINT       NOT NULL REFERENCES rooms(id),
    room_label       VARCHAR(60)  NOT NULL,
    category         VARCHAR(20)  NOT NULL
        CHECK (category IN ('ELECTRICAL','PLUMBING','FURNITURE','INTERNET','CLEANLINESS','OTHER')),
    priority         VARCHAR(10)  NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    description      VARCHAR(500) NOT NULL,
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED')),
    assigned_to      BIGINT REFERENCES users(id),
    resolution_notes VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

-- ---------- Announcements ----------
CREATE TABLE announcements (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(150) NOT NULL,
    body       VARCHAR(600) NOT NULL,
    author_id  BIGINT REFERENCES users(id),
    created_at TIMESTAMP    NOT NULL
);

-- ---------- Checkout ----------
CREATE TABLE checkout_requests (
    id              BIGSERIAL PRIMARY KEY,
    resident_id     BIGINT      NOT NULL REFERENCES residents(id),
    requested_on    DATE        NOT NULL,
    planned_date    DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','CLEARED','REJECTED')),
    reviewed_by     BIGINT REFERENCES users(id),
    review_note     VARCHAR(300),
    dues_at_review  NUMERIC(12,2)
);
CREATE UNIQUE INDEX uq_checkout_pending ON checkout_requests(resident_id) WHERE status = 'PENDING';
