#!/usr/bin/env bash

source test/truffle/common.sh.inc

jt ruby --experimental-options --check-compilation --engine.MultiTier=false test/truffle/compiler/stf-optimises/stf-optimises.rb
