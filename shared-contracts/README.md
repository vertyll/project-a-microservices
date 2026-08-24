# shared-contracts

Saga protocol types — `SagaTypeValue`, `SagaStatus`, `SagaStepStatus` — with the Kotlin standard
library as their only dependency.

They used to live in `shared-infrastructure`, which is a Spring module, so importing a single
saga enum pulled the whole framework onto an application layer that is supposed to be free of
it. Only the module boundary moved; the package names are unchanged, so no import had to.
