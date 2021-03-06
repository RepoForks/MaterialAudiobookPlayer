package de.ph1b.audiobook.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import com.bluelinelabs.conductor.*
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookOverview.BookShelfController
import de.ph1b.audiobook.features.bookOverview.NoFolderWarningDialogFragment
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.PermissionHelper
import de.ph1b.audiobook.misc.RouterProvider
import de.ph1b.audiobook.misc.asTransaction
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject


/**
 * Activity that coordinates the book shelf and play screens.
 *
 * @author Paul Woitaschek
 */
class MainActivity : BaseActivity(), NoFolderWarningDialogFragment.Callback, RouterProvider {

  private lateinit var permissionHelper: PermissionHelper
  @Inject lateinit var prefs: PrefsManager

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_book)
    App.component.inject(this)

    permissionHelper = PermissionHelper(this)

    val root = findViewById(R.id.root) as ViewGroup
    router = Conductor.attachRouter(this, root, savedInstanceState)
    if (!router.hasRootController()) {
      val rootTransaction = RouterTransaction.with(BookShelfController())
      router.setRoot(rootTransaction)
    }
    router.addChangeListener(object : ControllerChangeHandler.ControllerChangeListener {
      override fun onChangeStarted(to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) {
        from?.setOptionsMenuHidden(true)
      }

      override fun onChangeCompleted(to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) {
        from?.setOptionsMenuHidden(false)
      }
    })

    if (savedInstanceState == null) {
      if (intent.hasExtra(NI_GO_TO_BOOK)) {
        val bookId = intent.getLongExtra(NI_GO_TO_BOOK, -1)
        router.pushController(RouterTransaction.with(BookPlayController.newInstance(bookId)))
      }
    }
  }

  override fun provideRouter() = router

  override fun onStart() {
    super.onStart()

    val anyFolderSet = prefs.collectionFolders.value.size + prefs.singleBookFolders.value.size > 0
    if (anyFolderSet) {
      permissionHelper.storagePermission()
    }
  }

  override fun onBackPressed() {
    if (!router.handleBack()) super.onBackPressed()
  }

  companion object {
    private val NI_GO_TO_BOOK = "niGotoBook"

    /** Returns an intent that lets you go directly to the playback screen for a certain book **/
    fun goToBookIntent(c: Context, bookId: Long) = Intent(c, MainActivity::class.java).apply {
      putExtra(NI_GO_TO_BOOK, bookId)
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
  }

  override fun onNoFolderWarningConfirmed() {
    val transaction = FolderOverviewController().asTransaction()
    router.pushController(transaction)
  }
}
