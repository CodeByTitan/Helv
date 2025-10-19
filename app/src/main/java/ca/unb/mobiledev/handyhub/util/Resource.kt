package ca.unb.mobiledev.handyhub.util

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {

    // We'll wrap our data in this 'Success'
    // class in case of success response from api
    class Success<T>(data: T) : Resource<T>(data = data)

    // We'll pass error message wrapped in this 'Error'
    // class to the UI in case of failure response
    class Error<T>(message: String? = null) : Resource<T>( message = message)

    // We'll just pass object of this Loading
    // class, just before making an api call
    class Loading<Nothing> : Resource<Nothing>()
}