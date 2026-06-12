-- Route stops (delivery/pickup points) and operational event log.

CREATE TABLE route_stops (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    address varchar(512),
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_stops_firm_id ON route_stops(firm_id);

CREATE TABLE ops_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at timestamptz NOT NULL DEFAULT now(),
    prompt_excerpt text,
    response_excerpt text,
    model varchar(255)
);
