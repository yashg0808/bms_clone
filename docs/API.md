# BookMyShow Clone - API Documentation

## Base URL

- **Development:** `http://localhost:8080`
- **Production:** `https://api.bookmyshow.local`

---

## Movie Service (`/api/movies`)

### List Movies

```
GET /api/movies?page=0&size=20&genre=ACTION&language=HINDI&city=MUMBAI
```

**Query Params:** `page`, `size`, `genre`, `language`, `city`, `search`

**Response:** `200 OK`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "title": "Movie Title",
        "description": "...",
        "genre": "ACTION",
        "language": "HINDI",
        "durationMinutes": 150,
        "rating": 8.5,
        "posterUrl": "https://...",
        "releaseDate": "2024-01-15"
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "page": 0,
    "size": 20
  }
}
```

### Get Movie by ID

```
GET /api/movies/{id}
```

### Search Movies

```
GET /api/movies/search?q=avengers
```

---

## Theater Service (`/api/theaters`)

### List Theaters by City

```
GET /api/theaters?cityId={cityId}
```

### Get Theater

```
GET /api/theaters/{id}
```

---

## City Service (`/api/cities`)

### List Cities

```
GET /api/cities
```

---

## Show Service (`/api/shows`)

### Get Shows for Movie

```
GET /api/shows?movieId={movieId}&cityId={cityId}&date=2024-01-15
```

**Response:** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "movieId": "uuid",
      "screenId": "uuid",
      "screenName": "Screen 1",
      "theaterName": "PVR Phoenix",
      "showTime": "2024-01-15T14:30:00",
      "basePrice": 250.0,
      "availableSeats": 120
    }
  ]
}
```

### Get Show Seats

```
GET /api/shows/{showId}/seats
```

**Response:** `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "seatNumber": "A1",
      "rowName": "A",
      "seatType": "PREMIUM",
      "price": 350.0,
      "status": "AVAILABLE"
    }
  ]
}
```

---

## Booking Service (`/api/bookings`)

### Lock Seats

```
POST /api/bookings/lock
```

**Body:**

```json
{
  "showId": "uuid",
  "seatIds": ["uuid1", "uuid2"]
}
```

**Response:** `200 OK`

```json
{
  "success": true,
  "data": {
    "lockToken": "uuid",
    "bookingId": "uuid",
    "expiresAt": "2024-01-15T14:38:00",
    "lockedSeats": [{ "seatId": "uuid1", "seatNumber": "A1", "price": 350.0 }],
    "totalAmount": 700.0
  }
}
```

### Confirm Booking

```
POST /api/bookings/{bookingId}/confirm
```

**Body:**

```json
{
  "lockToken": "uuid",
  "guestName": "John Doe",
  "guestEmail": "john@example.com",
  "guestPhone": "+919876543210"
}
```

**Response:** `200 OK`

### Cancel Booking

```
POST /api/bookings/{bookingId}/cancel
```

### Get Booking

```
GET /api/bookings/{bookingId}
```

### Get Booking by Number

```
GET /api/bookings/number/{bookingNumber}
```

---

## Error Responses

All errors follow a consistent format:

```json
{
  "success": false,
  "message": "Error description",
  "errorCode": "SEAT_UNAVAILABLE",
  "timestamp": "2024-01-15T14:30:00"
}
```

### Common Status Codes

| Code | Description                                       |
| ---- | ------------------------------------------------- |
| 400  | Bad Request — Validation error                    |
| 404  | Not Found — Resource doesn't exist                |
| 409  | Conflict — Seat already locked, duplicate request |
| 429  | Too Many Requests — Rate limit exceeded           |
| 500  | Internal Server Error                             |
