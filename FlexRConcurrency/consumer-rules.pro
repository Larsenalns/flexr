# Règles R8 / ProGuard transmises aux applications qui consomment FlexR Concurrency.
#
# AUCUNE règle n'est nécessaire, et c'est vérifié : FlexR Concurrency ne désigne jamais une classe ni
# un membre par son nom. Les tâches sont manipulées par des lambdas et des Future, les modes par des
# valeurs d'énumération, et rien n'est instancié par réflexion.
#
# La règle précédente (`-keepnames class com.irisvision.flexrconcurrency.** { *; }`) a été RETIRÉE le
# 2026-10-15 : elle ne protégeait rien et empêchait l'obfuscation de tout le module.
# (Voir REVIEW/15-R8-REFLEXION.md §6.)
#
# Ce fichier est conservé, même vide de règles, pour rester aligné sur les autres modules et pour
# accueillir une règle future si un besoin réel apparaît — accompagné, cette fois, de sa justification.
