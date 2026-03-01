SHELL := /bin/bash

INFRA_PROJECT ?= my-commerce-infra
MON_PROJECT ?= my-commerce-mon

INFRA_COMPOSE_FILE := docker/infra-compose.yml
MON_COMPOSE_FILE := docker/monitoring-compose.yml

INFRA := docker compose -p $(INFRA_PROJECT) -f $(INFRA_COMPOSE_FILE)
MON := docker compose -p $(MON_PROJECT) -f $(MON_COMPOSE_FILE)

LEGACY_INFRA := docker compose -f $(INFRA_COMPOSE_FILE)
LEGACY_MON := docker compose -f $(MON_COMPOSE_FILE)
LEGACY_INFRA_NAMED := docker compose -p infra -f $(INFRA_COMPOSE_FILE)
LEGACY_MON_NAMED := docker compose -p mon -f $(MON_COMPOSE_FILE)

.PHONY: help up down ps infra-up infra-down infra-ps infra-logs monitor-up monitor-down monitor-ps monitor-logs preclean-legacy

help:
	@printf "Targets:\n"
	@printf "  make up            # infra + monitoring 한번에 실행(충돌 회피)\n"
	@printf "  make down          # infra + monitoring 종료\n"
	@printf "  make ps            # 전체 상태 확인\n"
	@printf "  make infra-up      # infra만 실행\n"
	@printf "  make monitor-up    # monitoring만 실행\n"

preclean-legacy:
	-$(LEGACY_MON_NAMED) down --remove-orphans
	-$(LEGACY_INFRA_NAMED) down --remove-orphans
	-$(LEGACY_MON) down --remove-orphans
	-$(LEGACY_INFRA) down --remove-orphans

infra-up:
	$(INFRA) up -d

infra-down:
	$(INFRA) down --remove-orphans

infra-ps:
	$(INFRA) ps

infra-logs:
	$(INFRA) logs -f

monitor-up:
	$(MON) up -d

monitor-down:
	$(MON) down --remove-orphans

monitor-ps:
	$(MON) ps

monitor-logs:
	$(MON) logs -f

up: preclean-legacy infra-up monitor-up ps

down: monitor-down infra-down

ps:
	@echo "== Infra ($(INFRA_PROJECT)) =="
	$(INFRA) ps
	@echo
	@echo "== Monitoring ($(MON_PROJECT)) =="
	$(MON) ps
