package com.example.api

import com.example.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/incomes")
    suspend fun getIncomes(): Response<List<Expense>>

    @POST("api/incomes")
    suspend fun createIncome(@Body request: Expense): Response<Expense>

    @GET("api/expenses")
    suspend fun getExpenses(): Response<List<Expense>>

    @POST("api/expenses")
    suspend fun createExpense(@Body request: Expense): Response<Expense>

    @GET("api/categories")
    suspend fun getCategories(): Response<List<Category>>

    @GET("api/goals")
    suspend fun getGoals(): Response<List<Goal>>

    @POST("api/goals")
    suspend fun createGoal(@Body request: Goal): Response<Goal>

    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("api/credit-cards")
    suspend fun getCreditCards(): Response<List<CreditCard>>

    @POST("api/credit-cards")
    suspend fun createCreditCard(@Body request: CreditCard): Response<CreditCard>

    @DELETE("api/credit-cards/{id}")
    suspend fun deleteCreditCard(@Path("id") id: Int): Response<Unit>

    @DELETE("api/users/me")
    suspend fun deleteAccount(): Response<Unit>
}
