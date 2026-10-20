# Règles R8 / ProGuard transmises aux applications qui consomment FlexR Theme.
#
# AUCUNE règle n'est nécessaire, et la justification précédente était inexacte : les observateurs de
# thème (Thematic) sont comparés par identité d'objet, et les identifiants de scope sont construits à
# partir d'INSTANCES ("<fragment>:<activity>"), jamais à partir de noms de classes. Rien n'est donc
# résolu par réflexion.
#
# La règle précédente (`-keepnames class com.irisvision.flexrthememodule.** { *; }`) a été RETIRÉE le
# 2026-10-15 : elle ne protégeait rien et empêchait l'obfuscation du module.
# (Voir REVIEW/15-R8-REFLEXION.md §6.)
