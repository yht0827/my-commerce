SHELL := /bin/bash

PROJECT ?= my-commerce
COMPOSE_FILE := docker/docker-compose.yml

DC := docker compose -p $(PROJECT) -f $(COMPOSE_FILE)

.PHONY: help up down restart ps logs preclean-legacy

help:
	@printf "Targets:\n"
	@printf "  make up       # 전체 스택 실행\n"
	@printf "  make down     # 전체 스택 종료\n"
	@printf "  make restart  # 전체 스택 재시작\n"
	@printf "  make ps       # 상태 확인\n"
	@printf "  make logs     # 전체 로그 스트리밍\n"

up:
	$(DC) up -d

down:
	$(DC) down --remove-orphans

restart:
	$(DC) down --remove-orphans
	$(DC) up -d

ps:
	$(DC) ps

logs:
	$(DC) logs -f
