package cpu

interface ProcessorCreator {
    fun create(configuration: ProcessorConfiguration): Processor
}

data object RealProcessorCreator : ProcessorCreator {
    override fun create(configuration: ProcessorConfiguration) = RealProcessor(configuration)
}
