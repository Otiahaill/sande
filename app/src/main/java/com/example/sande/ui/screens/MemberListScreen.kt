package com.example.sande.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sande.data.entities.Member
import com.example.sande.di.SandeApplication
import com.example.sande.ui.EmptyStateCard
import com.example.sande.ui.theme.SandeShape
import com.example.sande.ui.viewModelFactoryOf
import com.example.sande.ui.viewmodel.MemberListViewModel

@Composable
fun MemberListScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SandeApplication
    val vm: MemberListViewModel = viewModel(
        factory = viewModelFactoryOf { MemberListViewModel(app.memberRepository) }
    )
    val members by vm.members.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Members", style = MaterialTheme.typography.headlineSmall)
        if (members.isEmpty()) {
            EmptyStateCard(
                title = "No members yet",
                subtitle = "Team members will appear here",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members, key = { it.uid }) { member -> MemberRow(member) }
            }
        }
    }
}

@Composable
private fun MemberRow(member: Member) {
    val roleColor = when (member.role) {
        "OWNER" -> MaterialTheme.colorScheme.primary
        "MANAGER" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SandeShape.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(member.email ?: member.uid, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    member.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(member.role, style = MaterialTheme.typography.labelLarge, color = roleColor)
        }
    }
}
