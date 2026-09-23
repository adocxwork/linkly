--
-- PostgreSQL database dump
--

\restrict CfpEyBH3L4QQYHo5k9isF0IhoXd8vWLPh09XKM09idUCMEkbDayIljgXMrJ1oxr

-- Dumped from database version 18.3 (Homebrew)
-- Dumped by pg_dump version 18.3 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: links; Type: TABLE; Schema: public; Owner: adityagupta
--

CREATE TABLE public.links (
    id uuid NOT NULL,
    active boolean NOT NULL,
    click_count integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    original_url character varying(2048) NOT NULL,
    short_url character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL,
    sort_order integer DEFAULT 0
);


ALTER TABLE public.links OWNER TO adityagupta;

--
-- Name: messages; Type: TABLE; Schema: public; Owner: adityagupta
--

CREATE TABLE public.messages (
    id uuid NOT NULL,
    sender_name character varying(255),
    content text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.messages OWNER TO adityagupta;

--
-- Name: users; Type: TABLE; Schema: public; Owner: adityagupta
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    bio character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    username character varying(255) NOT NULL,
    is_suspended boolean DEFAULT false,
    role character varying(255) DEFAULT 'ROLE_USER'::character varying,
    upi_id character varying(255),
    enable_upi_payment boolean DEFAULT false,
    enable_public_messaging boolean DEFAULT false,
    theme character varying(255)
);


ALTER TABLE public.users OWNER TO adityagupta;

--
-- Name: links links_pkey; Type: CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.links
    ADD CONSTRAINT links_pkey PRIMARY KEY (id);


--
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: links ukpnuspv22vo934l20rw7pwio8w; Type: CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.links
    ADD CONSTRAINT ukpnuspv22vo934l20rw7pwio8w UNIQUE (short_url);


--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: links fki0o5fueopsnuoypws575u5yso; Type: FK CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.links
    ADD CONSTRAINT fki0o5fueopsnuoypws575u5yso FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: messages messages_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adityagupta
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict CfpEyBH3L4QQYHo5k9isF0IhoXd8vWLPh09XKM09idUCMEkbDayIljgXMrJ1oxr

