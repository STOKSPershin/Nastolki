package com.tbgames.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tbgames.app.feature.chat.presentation.ChatScreen
import com.tbgames.app.feature.chat.presentation.ChatViewModel
import com.tbgames.app.feature.gameroom.presentation.GameRoomScreen
import com.tbgames.app.feature.gameroom.presentation.GameRoomViewModel
import com.tbgames.app.feature.lobby.presentation.LobbyScreen
import com.tbgames.app.feature.lobby.presentation.LobbyViewModel
import com.tbgames.app.feature.onboarding.presentation.AvatarSelectionScreen
import com.tbgames.app.feature.onboarding.presentation.NicknameScreen
import com.tbgames.app.feature.onboarding.presentation.OnboardingViewModel
import com.tbgames.app.feature.onboarding.presentation.SplashScreen
import com.tbgames.app.feature.profile.presentation.ProfileSettingsScreen
import com.tbgames.app.feature.profile.presentation.ProfileViewModel

object Routes {
    const val SPLASH = "splash"
    const val NICKNAME = "nickname"
    const val AVATAR = "avatar"
    const val ACCOUNT_SELECTION = "account_selection"
    const val LOBBY = "lobby"
    const val PROFILE = "profile"
    const val CHAT = "chat"
    const val GAME_ROOM = "game_room/{roomId}"

    fun gameRoom(roomId: String) = "game_room/$roomId"
}

@Composable
fun TBGamesNavGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                onboardingViewModel.recheckLoginStatus()
            }
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Routes.NICKNAME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLobby = {
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToAccountSelection = {
                    navController.navigate(Routes.ACCOUNT_SELECTION) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                isLoggedIn = onboardingState.isLoggedIn,
                hasSavedAccounts = onboardingState.hasSavedAccounts
            )
        }

        composable(Routes.ACCOUNT_SELECTION) {
            com.tbgames.app.feature.onboarding.presentation.AccountSelectionScreen(
                accounts = onboardingState.savedAccounts,
                isLoading = onboardingState.isLoading,
                error = onboardingState.error,
                onAccountClick = onboardingViewModel::restoreAccount,
                onCreateNewAccountClick = {
                    onboardingViewModel.startNewAccountCreation()
                    navController.navigate(Routes.NICKNAME)
                },
                onDeleteAccountClick = onboardingViewModel::deleteSavedAccount
            )

            // Navigate to lobby when account is restored
            if (onboardingState.isLoggedIn == true) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.ACCOUNT_SELECTION) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.NICKNAME) {
            NicknameScreen(
                nickname = onboardingState.nickname,
                onNicknameChange = onboardingViewModel::onNicknameChange,
                nicknameError = onboardingState.nicknameError,
                isLoading = onboardingState.isLoading,
                onNext = {
                    onboardingViewModel.checkNicknameAndProceed()
                    if (onboardingState.nicknameError == null && onboardingState.nickname.length >= 3) {
                        navController.navigate(Routes.AVATAR)
                    }
                }
            )
        }

        composable(Routes.AVATAR) {
            AvatarSelectionScreen(
                selectedPresetId = onboardingState.selectedPresetId,
                onPresetSelected = onboardingViewModel::onPresetSelected,
                onCustomAvatarSelected = onboardingViewModel::onCustomAvatarSelected,
                isLoading = onboardingState.isLoading,
                error = onboardingState.error,
                onDone = {
                    onboardingViewModel.createProfile()
                }
            )

            // Navigate to lobby when profile is created
            if (onboardingState.isProfileCreated) {
                navController.navigate(Routes.LOBBY) {
                    popUpTo(Routes.NICKNAME) { inclusive = true }
                }
            }
        }

        composable(Routes.LOBBY) {
            val lobbyViewModel: LobbyViewModel = hiltViewModel()
            val lobbyState by lobbyViewModel.uiState.collectAsState()
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        lobbyViewModel.loadProfile()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // State-based navigation after room creation
            lobbyState.navigateToRoomId?.let { roomId ->
                lobbyViewModel.clearNavigation()
                navController.navigate(Routes.gameRoom(roomId))
            }

            LobbyScreen(
                state = lobbyState,
                onSettingsClick = { navController.navigate(Routes.PROFILE) },
                onChatClick = { navController.navigate(Routes.CHAT) },
                onCreateClick = { lobbyViewModel.showGameSelectDialog() },
                onHideGameSelectDialog = { lobbyViewModel.hideGameSelectDialog() },
                onSelectGame = { gameInfo ->
                    lobbyViewModel.createRoom(gameInfo)
                },
                onJoinRoom = { roomId ->
                    lobbyViewModel.joinRoom(roomId)
                    navController.navigate(Routes.gameRoom(roomId))
                },
                onRefreshClick = lobbyViewModel::loadRooms,
                onEnterLobby = lobbyViewModel::enterLobby
            )
        }

        composable(Routes.PROFILE) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val profileState by profileViewModel.uiState.collectAsState()

            // Handle logout navigation
            LaunchedEffect(profileState.loggedOut) {
                if (profileState.loggedOut) {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            ProfileSettingsScreen(
                state = profileState,
                onBackClick = { navController.popBackStack() },
                onStartEditNickname = profileViewModel::startEditNickname,
                onNicknameChange = profileViewModel::onEditNicknameChange,
                onSaveNickname = profileViewModel::saveNickname,
                onCancelEditNickname = profileViewModel::cancelEditNickname,
                onAvatarPresetSelected = profileViewModel::onAvatarPresetSelected,
                onCustomAvatarSelected = profileViewModel::onCustomAvatarSelected,
                onToggleSound = profileViewModel::toggleSound,
                onToggleVibration = profileViewModel::toggleVibration,
                onThemeModeChange = profileViewModel::setThemeMode,
                onShowLogoutDialog = profileViewModel::showLogoutDialog,
                onHideLogoutDialog = profileViewModel::hideLogoutDialog,
                onLogout = profileViewModel::logout
            )
        }

        composable(Routes.CHAT) {
            val chatViewModel: ChatViewModel = hiltViewModel()
            val chatState by chatViewModel.uiState.collectAsState()

            ChatScreen(
                state = chatState,
                onBackClick = { navController.popBackStack() },
                onMessageChange = chatViewModel::onMessageChange,
                onSendMessage = chatViewModel::sendMessage
            )
        }

        composable(Routes.GAME_ROOM) {
            val gameRoomViewModel: GameRoomViewModel = hiltViewModel()
            val gameRoomState by gameRoomViewModel.uiState.collectAsState()


            GameRoomScreen(
                state = gameRoomState,
                onBackClick = { navController.popBackStack() },
                onToggleRules = gameRoomViewModel::toggleRules,
                onTransferHost = gameRoomViewModel::transferHost,
                onLeaveRoom = gameRoomViewModel::leaveRoom,
                onSettingsChange = gameRoomViewModel::updateSettings,
                onStartReadyCheck = gameRoomViewModel::startReadyCheck,
                onCancelReadyCheck = gameRoomViewModel::cancelReadyCheck,
                onToggleReady = gameRoomViewModel::toggleReady,
                onStartGame = gameRoomViewModel::startGame,
                onRoundResult = gameRoomViewModel::handleRoundResult,
                onUpdateGameState = gameRoomViewModel::updateGameState,
                onSubmitPasswordWord = gameRoomViewModel::submitPasswordWord,
                onAwardPasswordPoints = gameRoomViewModel::awardPasswordPoints,
                onEndGameForAll = gameRoomViewModel::endGameForAll,
                onDismissDisconnectDialog = gameRoomViewModel::dismissDisconnectDialog
            )
        }
    }
}
