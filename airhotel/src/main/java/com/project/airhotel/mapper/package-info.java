/**
 * Mappers that convert domain entities to API-facing DTOs and vice versa.
 * Centralizes transformation logic, field selection, and format normalization
 * (for example, converting date-time fields to UTC Instants, handling nulls,
 * and shaping detail vs. summary projections). Mappers are stateless and
 * thread-safe and can be extended as new DTOs or fields are introduced.
 */
package com.project.airhotel.mapper;
