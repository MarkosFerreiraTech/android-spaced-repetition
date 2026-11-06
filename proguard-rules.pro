# Regras do R8 para o build de release.
# Room e OpenCSV usam reflexao; manter seus metadados.

-keep class com.opencsv.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Room gera implementacoes em tempo de compilacao
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Manter nomes de enums serializados no banco (ordinal)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
