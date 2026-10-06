package cz.sazel.arch.presentation

/**
 * The presentation of an operation done in one step: it calls the use cases, turns what they answer
 * into text and shows it itself.
 *
 * The lighter counterpart of a view model. Nothing is kept between two calls, so there is no state
 * to export. A flow of several steps, or one that waits for the person in between, gets a view
 * model instead.
 */
fun interface ILogicHandler<in INPUT> {

    suspend operator fun invoke(input: INPUT)
}
