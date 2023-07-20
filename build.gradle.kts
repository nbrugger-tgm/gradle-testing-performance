import groovy.json.JsonOutput

tasks.register("run") {
    dependsOn(allTestTasks)
    doLast {
        logger.info(JsonOutput.toJson(aggregate))
        println(JsonOutput.toJson(aggregate))
        if (!file("$buildDir/report.json").exists()) {
            if(!file("$buildDir").exists()) {
                file("$buildDir").mkdirs()
            }
            file("$buildDir/report.json").createNewFile();
        }
        file("$buildDir/report.json").writeText(JsonOutput.toJson(aggregate));
    }
}
val unitTests = configureModule("unit-only");
val allTestTasks = arrayOf(unitTests).flatMap { e -> e };
val aggregate = mutableMapOf<String, MutableMap<String, Map<String, Long>>>()
val times = mutableMapOf<String, Long>()

fun configureModule(mod: String): Set<TaskProvider<*>> {
    val rootMod = project(":$mod")
    val testTasks = mutableSetOf<TaskProvider<*>>()
    val quantityCfg = {
        setOf(
            TestConfig(TestQuantity.LOW, ModuleQuantity.SINGLE, true),
            TestConfig(TestQuantity.MEDIUM, ModuleQuantity.SINGLE, true),
            TestConfig(TestQuantity.HIGH, ModuleQuantity.SINGLE, true),
            TestConfig(TestQuantity.HIGH, ModuleQuantity.SINGLE, false),
            TestConfig(TestQuantity.LOW, ModuleQuantity.SINGLE, false),
            TestConfig(TestQuantity.MEDIUM, ModuleQuantity.SINGLE, false),
            TestConfig(TestQuantity.MEDIUM, ModuleQuantity.FEW, true),
            TestConfig(TestQuantity.HIGH, ModuleQuantity.FEW, true),
            TestConfig(TestQuantity.HIGH, ModuleQuantity.MANY, true),
            TestConfig(TestQuantity.MEDIUM, ModuleQuantity.FEW, false),
            TestConfig(TestQuantity.HIGH, ModuleQuantity.FEW, false),
            TestConfig(TestQuantity.HIGH, ModuleQuantity.MANY, false)
        )
    }
    for (cfg in quantityCfg()) {
        rootMod.subprojects {
            afterEvaluate {
                val java = extensions.getByType<JavaPluginExtension>()
                tasks.register<Test>(cfg.toInModuleString()) {
                    filter {
                        for (pattern in cfg.whitelist()) {
                            includeTestsMatching(pattern)
                        }
                    }
                    logger.lifecycle("Configuring $cfg : ${filter.includePatterns}")
                    var start = 0L;
                    doFirst {
                        start = System.currentTimeMillis()
                    }
                    doLast {
                        val time = System.currentTimeMillis() - start
                        times[this.path] = time
                    }
                    useJUnitPlatform()
                    group = "generated-tests"
                    this.classpath = java.sourceSets.getByName("test").runtimeClasspath
                    outputs.upToDateWhen { false }
                }
            }
        }
        testTasks.add(rootMod.tasks.register(cfg.toString()) {
            group = "generated-tests"
            val subtasks = 1.rangeTo(cfg.modules.i).map {
                project(":$mod:module$it").tasks.named(cfg.toInModuleString())
            }
            dependsOn(subtasks)
            doLast {
                val time = times.entries
                    .filter { subtasks.map { t -> t.get().path }.contains(it.key) }
                    .sumOf { it.value }
                val modAggregate = aggregate[mod] ?: mutableMapOf()
                modAggregate[cfg.toString()] = metrics.associateBy({ it.getName() }, { it.calculate(cfg, time) })
                aggregate[mod] = modAggregate
            }
        })
    }
    rootMod.tasks.register("run") {
        dependsOn(testTasks)
        doLast {
            logger.info(JsonOutput.toJson(aggregate))
            println(JsonOutput.toJson(aggregate))
            if (!file("$buildDir/report.json").exists()) {
                if(!file("$buildDir").exists()) {
                    file("$buildDir").mkdirs()
                }
                file("$buildDir/report.json").createNewFile();
            }
            file("$buildDir/report.json").writeText(JsonOutput.toJson(aggregate));
        }
    }
    return testTasks;
}

data class TestConfig(
    val tests: TestQuantity,
    val modules: ModuleQuantity,
    val singleMethod: Boolean
) {
    override fun toString(): String {
        return modules.name.lowercase() + "-modules_" + tests.name.lowercase() + "-tests" + (if (singleMethod) "" else "_ten-methods")
    }

    fun whitelist(): List<String> {
        return 1.rangeTo(tests.i)
            .map { i -> "*TestSet$i" }
            .map { i -> if (singleMethod)  "$i.testAction_2*" else "$i.testAction*" } //only the 9th method
            .toList()
    }

    fun toInModuleString(): String {
        return "${tests.name.lowercase()}-tests${if (singleMethod) "" else "_ten-methods"}_for-${modules.name.lowercase()}";
    }
}

interface Metric {
    fun getName(): String
    fun calculate(cfg: TestConfig, ms: Long): Long

    companion object {
        fun create(name: String, calc: (cfg: TestConfig, ms: Long) -> Long): Metric {
            return object : Metric {
                override fun getName(): String {
                    return name
                }

                override fun calculate(cfg: TestConfig, ms: Long): Long {
                    return calc(cfg, ms);
                }
            }
        }
    }
}

enum class TestQuantity(val i: Int) {
    LOW(1),
    MEDIUM(50),
    HIGH(200)
}

enum class ModuleQuantity(val i: Int) {
    SINGLE(1),
    FEW(5),
    MANY(20)
}

val metrics = setOf(
    Metric.create("time per module") { cfg, ms -> ms / cfg.modules.i },
    Metric.create("time per test class") { cfg, ms ->
        ms / (cfg.modules.i * cfg.tests.i)
    },
    Metric.create("time per test method") { cfg, ms ->
        ms / (cfg.modules.i * cfg.tests.i * (if (cfg.singleMethod) 1 else 10))
    },
    Metric.create("time") { _, ms -> ms },
    Metric.create("test classes") { cfg, _ ->
        (cfg.modules.i * cfg.tests.i).toLong()
    },
    Metric.create("test methods") { cfg, _ ->
        cfg.modules.i * cfg.tests.i * (if (cfg.singleMethod) 1 else 10).toLong()
    }
);