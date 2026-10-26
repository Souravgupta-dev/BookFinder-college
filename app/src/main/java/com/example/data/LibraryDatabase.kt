package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [BookEntity::class, UserEntity::class, SearchQueryEntity::class, UserBookStateEntity::class, BookReservationEntity::class], version = 9, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao
  abstract fun userDao(): UserDao
  abstract fun searchQueryDao(): SearchQueryDao
  abstract fun userBookStateDao(): UserBookStateDao
  abstract fun bookReservationDao(): BookReservationDao

  companion object {
    @Volatile private var INSTANCE: LibraryDatabase? = null

    fun getDatabase(context: Context): LibraryDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LibraryDatabase::class.java,
          "library_database"
        )
          .fallbackToDestructiveMigration()
          .addCallback(DatabaseCallback())
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          CoroutineScope(Dispatchers.IO).launch {
            populateInitialData(database.bookDao(), database.bookReservationDao())
          }
        }
      }
    }

    suspend fun populateInitialData(dao: BookDao, resDao: BookReservationDao? = null) {
      val initialBooks = listOf(
        // Computer Science
        BookEntity(
          title = "Introduction to Algorithms, 4th Edition",
          authors = "Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein",
          category = "Computer Science",
          isbn = "978-0262046305",
          callNumber = "QA76.6 .C662 2022",
          floor = "Floor 2",
          wing = "North Wing",
          rack = "CS-04",
          shelf = "Shelf 3",
          status = "AVAILABLE",
          totalCopies = 5,
          availableCopies = 4,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
          synopsis = "A comprehensive modern textbook on computer algorithms, covering a wide array of data structures, graph algorithms, dynamic programming, and greedy methods.",
          isSaved = true
        ),
        BookEntity(
          title = "Artificial Intelligence: A Modern Approach, 4th Edition",
          authors = "Stuart Russell, Peter Norvig",
          category = "Computer Science",
          isbn = "978-0134610993",
          callNumber = "Q335 .R85 2020",
          floor = "Floor 2",
          wing = "AI & Data Wing",
          rack = "CS-01",
          shelf = "Shelf 1",
          status = "AVAILABLE",
          totalCopies = 6,
          availableCopies = 5,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCQ8CI3w-GUBYgPP4JaU71Pbwy22e2thtcRjjjvrlwk7cvQ_yVtDGBjakkTeb98baWhqnAS6IwjBYDn5VzjLzcTMB2cx2yw2gqbiJU2hgZWdocMTffI_jInGOKf6WnwKiaA2XwQmtsLbYLwJalmUvudssORG80WaHN6BSgoPIH0vqtdpBEknIE888_qkHqQ3PAI6CnfnJjpo1StLpGEGNQaZvyizDIms772Sn3ZIOl1fHzJi0QNq2ic",
          synopsis = "The definitive textbook on artificial intelligence, covering search algorithms, machine learning, probabilistic reasoning, deep learning, and robotics.",
          isSaved = true
        ),
        BookEntity(
          title = "Database System Concepts, 7th Edition",
          authors = "Abraham Silberschatz, Henry F. Korth, S. Sudarshan",
          category = "Computer Science",
          isbn = "978-0078022159",
          callNumber = "QA76.9.D3 S55 2019",
          floor = "Floor 2",
          wing = "North Wing",
          rack = "CS-03",
          shelf = "Shelf 2",
          status = "AVAILABLE",
          totalCopies = 4,
          availableCopies = 3,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDvbseK6oatHilmCittdyt1sTNkleEUhde8CKsca1qrGabVBEyyvyuH3Gwz3MjG21-Lf4E2D_BVfT6JJ6_GkPg9W8yl0j9SwyT-D-hx6tE81Tvv8agXvSxd_roSu1kWjDxSd75U83kLmYSz5ozClDLdePl_TXjXRUshEEL4EsEABCc1KBDscxUYhH-JXcjLOh8ueAFbYyBVMD9CkWwlqk8bxNfZ7va0_WnYxV4Y65fq_25apVo5s3w4",
          synopsis = "Presents the fundamental concepts of database management systems, covering SQL, transaction processing, data storage, and distributed databases.",
          isSaved = false
        ),
        BookEntity(
          title = "Clean Code: A Handbook of Agile Software Craftsmanship",
          authors = "Robert C. Martin",
          category = "Computer Science",
          isbn = "978-0132350884",
          callNumber = "QA76.76 .C64",
          floor = "Floor 1",
          wing = "Engineering Wing",
          rack = "CS-02",
          shelf = "Shelf 5",
          status = "ISSUED",
          totalCopies = 2,
          availableCopies = 0,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCQ8CI3w-GUBYgPP4JaU71Pbwy22e2thtcRjjjvrlwk7cvQ_yVtDGBjakkTeb98baWhqnAS6IwjBYDn5VzjLzcTMB2cx2yw2gqbiJU2hgZWdocMTffI_jInGOKf6WnwKiaA2XwQmtsLbYLwJalmUvudssORG80WaHN6BSgoPIH0vqtdpBEknIE888_qkHqQ3PAI6CnfnJjpo1StLpGEGNQaZvyizDIms772Sn3ZIOl1fHzJi0QNq2ic",
          synopsis = "Even bad code can function. But if code isn't clean, it can bring a development organization to its knees.",
          isSaved = true,
          isIssued = true,
          dueDate = "Oct 24"
        ),
        // Mathematics & Stats
        BookEntity(
          title = "Linear Algebra and Its Applications",
          authors = "Gilbert Strang",
          category = "Mathematics & Stats",
          isbn = "978-0321982384",
          callNumber = "QA184.2 .S77",
          floor = "Floor 2",
          wing = "Central Wing",
          rack = "MA-02",
          shelf = "Shelf 1",
          status = "AVAILABLE",
          totalCopies = 3,
          availableCopies = 2,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDvbseK6oatHilmCittdyt1sTNkleEUhde8CKsca1qrGabVBEyyvyuH3Gwz3MjG21-Lf4E2D_BVfT6JJ6_GkPg9W8yl0j9SwyT-D-hx6tE81Tvv8agXvSxd_roSu1kWjDxSd75U83kLmYSz5ozClDLdePl_TXjXRUshEEL4EsEABCc1KBDscxUYhH-JXcjLOh8ueAFbYyBVMD9CkWwlqk8bxNfZ7va0_WnYxV4Y65fq_25apVo5s3w4",
          synopsis = "Renowned linear algebra text emphasizing foundational concepts and practical matrix applications across engineering and sciences.",
          isSaved = true
        ),
        BookEntity(
          title = "Calculus: Early Transcendentals",
          authors = "James Stewart",
          category = "Mathematics & Stats",
          isbn = "978-1285740621",
          callNumber = "QA303.2 .S74 2016",
          floor = "Floor 2",
          wing = "Mathematics Wing",
          rack = "MA-01",
          shelf = "Shelf 5",
          status = "AVAILABLE",
          totalCopies = 4,
          availableCopies = 4,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCThI6X1bcHC5BFo4cbbfYQti2beodSjUw9MClE61i0-3-2ytBzQgEN0XdqGdJskA2gQHOO_CQFeX2-KvBVTnbsalzAJ_-JaN96a2DM3CmvcRnueudE0Ulc_JtXCpJTBoTBcZayGpybtU62SuImt00WR8aS77xb_omHLFbKKgSNJJykimBDGTdN9OPO6CfRu2TI1s3r8zZwuqVBzFgxciUHH6hf94oPKEQ_Mh_az7aaPpXrjBBqgVou",
          synopsis = "Comprehensive calculus textbook featuring mathematical rigor, problem-solving strategies, and real-world applications.",
          isSaved = false
        ),
        // Engineering & Tech
        BookEntity(
          title = "Standard Handbook for Mechanical Engineers",
          authors = "Ali Sadegh, William M. Rice",
          category = "Engineering & Tech",
          isbn = "978-0071832650",
          callNumber = "TJ151 .S25 2018",
          floor = "Floor 3",
          wing = "Engineering Wing",
          rack = "ENG-01",
          shelf = "Shelf 2",
          status = "AVAILABLE",
          totalCopies = 3,
          availableCopies = 3,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCfDeJ5Ad_oa55o0Gr7_m0Y0FpzuU8pqpQp_M5OTLZ-sHXFtBkVtArIgsGTaVAa9OdnYg4oHw5KV-l76b7GBaY62zg1aTo4EMwcE0x_n3bH2WUlXij5GNH19ramuxI5YBUvSDtt5UfyT4blDwQ1F5Ka3nu9X8eIRgvZ4YIhwMfb4s8jsLJ393_rKbwI1hgq0pswXTE9Wew6YlGriplXb-PO-xECyROytKm9hDkVqMSVZ4WqUNoThOVC",
          synopsis = "The ultimate authority in mechanical engineering, offering comprehensive data, formulas, standards, and principles for modern design.",
          isSaved = false
        ),
        BookEntity(
          title = "Microelectronic Circuits",
          authors = "Adel S. Sedra, Kenneth C. Smith",
          category = "Engineering & Tech",
          isbn = "978-0199339135",
          callNumber = "TK7867 .S43 2014",
          floor = "Floor 3",
          wing = "Electrical Wing",
          rack = "ENG-03",
          shelf = "Shelf 1",
          status = "AVAILABLE",
          totalCopies = 4,
          availableCopies = 2,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCOtSHMTg5BZgsZyg5OZXgcJqbwcDeGrv7LVP-6ch9-JbOYVHL9uOoMb6jRwJZt0lKhoD9E5CgoIci6sO579TyXglVKC4Sa7lw2pmseCDb5YOM50awZ-XWQTSHZd9jUXHWA86qXUecQ0cFmyoGpttXfOURe3O_jSs8Y8Racwyjy0JTVf1C04yR_VyKxeeksyn5sgb_Ime9JuZTMsWZIQvWPbM1xrONguNQL84Ay6m4ixYFqpgMj8dtW",
          synopsis = "Widely regarded as the most comprehensive and rigorous text available for undergraduate microelectronics.",
          isSaved = false
        ),
        // Natural Sciences
        BookEntity(
          title = "University Physics with Modern Physics",
          authors = "Hugh D. Young, Roger A. Freedman",
          category = "Natural Sciences",
          isbn = "978-0135159550",
          callNumber = "QC21.3 .Y68 2020",
          floor = "Floor 3",
          wing = "Science Wing",
          rack = "SCI-01",
          shelf = "Shelf 4",
          status = "AVAILABLE",
          totalCopies = 5,
          availableCopies = 5,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB5Hmd79wsve2_LPkRZqAGUUA4Yc2B5Z9skSay51bgzpGornjLy9Q5xiqHbTj5c5NJ99LHv3JjnEUioHA3G4c00dyaKMo6WLfCKpBF42Y2OVdadH3WWeoDLlRL4ybMLqgdg4sxm8onGcei1EQnJ_tPKXhewB7_sdKspDBkYm-AFB3ZzxkMhTl2edysVqbP48YAO1GlLoCnNfIpqVFA_ytBSHUwHHDIhRJT12D-ribZx1nf5RL51j1wP",
          synopsis = "The preeminent calculus-based physics text known for rigorous problem-solving methodology and rich conceptual depth.",
          isSaved = false
        ),
        BookEntity(
          title = "Principles of Anatomy and Physiology",
          authors = "Gerard J. Tortora, Bryan H. Derrickson",
          category = "Natural Sciences",
          isbn = "978-1119662793",
          callNumber = "QP34.5 .T67 2020",
          floor = "Floor 3",
          wing = "Biology Wing",
          rack = "SCI-05",
          shelf = "Shelf 2",
          status = "AVAILABLE",
          totalCopies = 3,
          availableCopies = 2,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
          synopsis = "An authoritative guide to human anatomy and physiological systems with gorgeous illustrations and clinical correlations.",
          isSaved = false
        ),
        // Literature & Arts
        BookEntity(
          title = "The Norton Anthology of English Literature",
          authors = "Stephen Greenblatt, M. H. Abrams",
          category = "Literature & Arts",
          isbn = "978-0393603125",
          callNumber = "PR1109 .N6 2018",
          floor = "Floor 1",
          wing = "Humanities Wing",
          rack = "LIT-01",
          shelf = "Shelf 1",
          status = "AVAILABLE",
          totalCopies = 4,
          availableCopies = 4,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCQ8CI3w-GUBYgPP4JaU71Pbwy22e2thtcRjjjvrlwk7cvQ_yVtDGBjakkTeb98baWhqnAS6IwjBYDn5VzjLzcTMB2cx2yw2gqbiJU2hgZWdocMTffI_jInGOKf6WnwKiaA2XwQmtsLbYLwJalmUvudssORG80WaHN6BSgoPIH0vqtdpBEknIE888_qkHqQ3PAI6CnfnJjpo1StLpGEGNQaZvyizDIms772Sn3ZIOl1fHzJi0QNq2ic",
          synopsis = "The trusted anthology for English literature, featuring comprehensive selections of major works across eras and movements.",
          isSaved = false
        ),
        BookEntity(
          title = "The Story of Art",
          authors = "E. H. Gombrich",
          category = "Literature & Arts",
          isbn = "978-0714833552",
          callNumber = "N5300 .G65 1995",
          floor = "Floor 1",
          wing = "Arts & Design Wing",
          rack = "ART-02",
          shelf = "Shelf 3",
          status = "AVAILABLE",
          totalCopies = 3,
          availableCopies = 3,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDvbseK6oatHilmCittdyt1sTNkleEUhde8CKsca1qrGabVBEyyvyuH3Gwz3MjG21-Lf4E2D_BVfT6JJ6_GkPg9W8yl0j9SwyT-D-hx6tE81Tvv8agXvSxd_roSu1kWjDxSd75U83kLmYSz5ozClDLdePl_TXjXRUshEEL4EsEABCc1KBDscxUYhH-JXcjLOh8ueAFbYyBVMD9CkWwlqk8bxNfZ7va0_WnYxV4Y65fq_25apVo5s3w4",
          synopsis = "One of the most famous and popular books on art ever written, beloved across the world for its clear language and engaging narrative.",
          isSaved = false
        ),
        // Business & Mgmt
        BookEntity(
          title = "Principles of Corporate Finance",
          authors = "Richard A. Brealey, Stewart C. Myers, Franklin Allen",
          category = "Business & Mgmt",
          isbn = "978-1260013900",
          callNumber = "HG4026 .B74 2020",
          floor = "Floor 1",
          wing = "Business Wing",
          rack = "BUS-01",
          shelf = "Shelf 2",
          status = "AVAILABLE",
          totalCopies = 4,
          availableCopies = 3,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCThI6X1bcHC5BFo4cbbfYQti2beodSjUw9MClE61i0-3-2ytBzQgEN0XdqGdJskA2gQHOO_CQFeX2-KvBVTnbsalzAJ_-JaN96a2DM3CmvcRnueudE0Ulc_JtXCpJTBoTBcZayGpybtU62SuImt00WR8aS77xb_omHLFbKKgSNJJykimBDGTdN9OPO6CfRu2TI1s3r8zZwuqVBzFgxciUHH6hf94oPKEQ_Mh_az7aaPpXrjBBqgVou",
          synopsis = "Describes the theory and practice of corporate finance with practical insights on financial decision making and capital markets.",
          isSaved = false
        ),
        BookEntity(
          title = "Marketing Management, 15th Edition",
          authors = "Philip Kotler, Kevin Lane Keller",
          category = "Business & Mgmt",
          isbn = "978-0133856460",
          callNumber = "HF5415.13 .K68 2016",
          floor = "Floor 1",
          wing = "Business Wing",
          rack = "BUS-03",
          shelf = "Shelf 1",
          status = "AVAILABLE",
          totalCopies = 3,
          availableCopies = 2,
          coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCfDeJ5Ad_oa55o0Gr7_m0Y0FpzuU8pqpQp_M5OTLZ-sHXFtBkVtArIgsGTaVAa9OdnYg4oHw5KV-l76b7GBaY62zg1aTo4EMwcE0x_n3bH2WUlXij5GNH19ramuxI5YBUvSDtt5UfyT4blDwQ1F5Ka3nu9X8eIRgvZ4YIhwMfb4s8jsLJ393_rKbwI1hgq0pswXTE9Wew6YlGriplXb-PO-xECyROytKm9hDkVqMSVZ4WqUNoThOVC",
          synopsis = "The gold standard marketing text providing global perspectives, strategic frameworks, and modern digital marketing strategies.",
          isSaved = false
        )
      )
      dao.insertAll(initialBooks)
      resDao?.let { rDao ->
        val sampleReservations = listOf(
          BookReservationEntity(
            bookId = 1L,
            bookTitle = "Introduction to Algorithms, 4th Edition",
            bookAuthors = "Thomas H. Cormen, Charles E. Leiserson",
            bookCoverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
            bookCallNumber = "QA76.6 .C662 2022",
            userEmail = "student@university.edu",
            userName = "Alex Rivera (Student ID: CS-2024)",
            status = "PENDING",
            pickupLocation = "Main Circulation Desk - Floor 1",
            studentNotes = "Needed for Advanced Data Structures project midterm.",
            adminComment = ""
          ),
          BookReservationEntity(
            bookId = 2L,
            bookTitle = "Artificial Intelligence: A Modern Approach, 4th Edition",
            bookAuthors = "Stuart Russell, Peter Norvig",
            bookCoverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCQ8CI3w-GUBYgPP4JaU71Pbwy22e2thtcRjjjvrlwk7cvQ_yVtDGBjakkTeb98baWhqnAS6IwjBYDn5VzjLzcTMB2cx2yw2gqbiJU2hgZWdocMTffI_jInGOKf6WnwKiaA2XwQmtsLbYLwJalmUvudssORG80WaHN6BSgoPIH0vqtdpBEknIE888_qkHqQ3PAI6CnfnJjpo1StLpGEGNQaZvyizDIms772Sn3ZIOl1fHzJi0QNq2ic",
            bookCallNumber = "Q335 .R85 2020",
            userEmail = "sarah.chen@university.edu",
            userName = "Sarah Chen (Student ID: AI-4091)",
            status = "APPROVED",
            pickupLocation = "Science & Tech Counter - Floor 2",
            studentNotes = "Thesis research on probabilistic reasoning.",
            adminComment = "Approved. Reserved for pickup until Friday 5:00 PM."
          )
        )
        rDao.insertAll(sampleReservations)
      }
    }
  }
}
