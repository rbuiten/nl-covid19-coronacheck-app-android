package nl.rijksoverheid.ctr.holder.ui.create_qr.usecases

import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.repositories.EventProviderRepository
import nl.rijksoverheid.ctr.shared.models.NetworkRequestResult

/**
 * Get events for a event provider
 */
interface GetRemoteEventsUseCase {
    suspend fun getRemoteEvents(
        eventProvider: RemoteConfigProviders.EventProvider,
        filter: String,
        token: RemoteAccessTokens.Token
    ): RemoteEventsResult
}

class GetRemoteEventsUseCaseImpl(private val eventProviderRepository: EventProviderRepository) :
    GetRemoteEventsUseCase {

    override suspend fun getRemoteEvents(
        eventProvider: RemoteConfigProviders.EventProvider,
        filter: String,
        token: RemoteAccessTokens.Token
    ): RemoteEventsResult {

        return when (val eventsResult = eventProviderRepository
            .getEvents(
                url = eventProvider.eventUrl,
                token = token.event,
                filter = filter,
                signingCertificateBytes = eventProvider.cms,
                provider = eventProvider.providerIdentifier,
            )) {

            is NetworkRequestResult.Success<SignedResponseWithModel<RemoteProtocol3>> ->
                RemoteEventsResult.Success(eventsResult.response)
            is NetworkRequestResult.Failed -> RemoteEventsResult.Error(eventsResult)
        }
    }
}
